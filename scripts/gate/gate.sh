#!/usr/bin/env bash
# Haru 품질 게이트 — 로컬·훅·CI가 같은 규칙을 쓴다.
#
#   gate.sh staged              pre-commit: 스테이징 변경의 정책 검사(수 초)
#   gate.sh push     [base]     pre-push: 커밋된 변경(base...HEAD) 정책 + 바뀐 모듈 check + diff 커버리지
#   gate.sh worktree [base]     커밋 전 작업 트리 기준(팀원 완료 조건). push와 같은 검사
#   gate.sh full     [base]     모든 모듈 + sql + 통합 레인(환경 있으면) + E2E(도커 있으면)
#   gate.sh e2e                 compose 전 스택 + Newman 회귀만
#
# base 기본값: origin/main. 실패한 단계가 있어도 끝까지 돌고 마지막에 요약 후 비정상 종료한다.
set -uo pipefail

# git 훅(pre-push 등) 안에서 실행되면 git이 GIT_DIR·GIT_INDEX_FILE 등을 설정해 둔다. 이를 물려받으면
# 다른 worktree에서 실행한 git 명령이 원래 저장소의 HEAD·인덱스를 조작한다(PR #10에서 자가 시험 커밋이 브랜치에 섞임).
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_PREFIX GIT_COMMON_DIR GIT_OBJECT_DIRECTORY \
      GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_NAMESPACE GIT_QUARANTINE_PATH

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
MODE=${1:-worktree}
BASE=${2:-origin/main}
GATE_DIR="$ROOT/scripts/gate"
LOG_DIR=${GATE_LOG_DIR:-"$ROOT/build/gate"}
mkdir -p "$LOG_DIR"

# 같은 작업 트리에서 게이트가 동시에 돌면 build/·coverage/ 산출물을 서로 지운다 → 작업 트리 단위 직렬화.
# (pre-commit의 staged 모드는 산출물을 쓰지 않으므로 잠그지 않는다)
if [ "$MODE" != staged ] && command -v flock >/dev/null; then
  mkdir -p "$ROOT/build"
  exec 9>"$ROOT/build/.gate.lock"
  if ! flock -n 9; then
    echo "… 같은 작업 트리에서 다른 게이트가 실행 중이다. 끝날 때까지 기다린다(최대 40분)"
    flock -w 2400 9 || { echo "게이트 잠금 대기 시간 초과"; exit 2; }
  fi
fi

declare -a RESULTS=()
FAILED=0

step() { # step <name> <command...>
  local name=$1; shift
  local log="$LOG_DIR/${name//[^A-Za-z0-9_.-]/_}.log"
  local t0=$SECONDS
  printf '▶ %-28s ' "$name"
  if "$@" >"$log" 2>&1; then
    printf 'ok   (%ss)\n' $((SECONDS - t0))
    RESULTS+=("ok   $name")
  else
    printf 'FAIL (%ss) → %s\n' $((SECONDS - t0)) "${log#"$ROOT"/}"
    tail -n 40 "$log" | sed 's/^/    │ /'
    RESULTS+=("FAIL $name")
    FAILED=1
  fi
}

skip() { printf '▷ %-28s skip — %s\n' "$1" "$2"; RESULTS+=("skip $1 ($2)"); }

summary() {
  echo "──────── gate($MODE) 요약"
  printf '  %s\n' "${RESULTS[@]}"
  if [ "$FAILED" -ne 0 ]; then
    echo "✗ 게이트 실패. 위 로그를 고친 뒤 다시 실행한다. 우회(--no-verify, 테스트 삭제/비활성, 하한 하향)는 정책 위반이다."
    exit 1
  fi
  echo "✓ 게이트 통과"
}

changed_paths() {
  local mb
  mb=$(git merge-base "$BASE" HEAD)
  if [ "$MODE" = "push" ]; then
    git diff --name-only "$mb" HEAD
  else
    git diff --name-only "$mb"
    git ls-files --others --exclude-standard
  fi
}

gradle_check() {
  (cd "$ROOT/$1" && ./gradlew check --console=plain --no-daemon -q) &&
    # 조건부 스킵으로 일부 테스트가 조용히 빠지는 것도 실패로 본다(CI와 동일, H01.6 R6)
    python3 "$GATE_DIR/junit_summary.py" "$ROOT/$1/build/test-results/test" --min 1 --max-skipped 0
}
sql_test() {
  # 단위 리포트에는 이번 실행의 단위 테스트만 넣는다. MySQL 레인은 통합 단계가 돌 때 다시 합친다(H01.6 R7, H01.7 N4)
  rm -f "$ROOT/sql/build/jacoco/mysqlIntegrationTest.exec"
  "$ROOT/CoreService/gradlew" -p "$ROOT/sql" test jacocoTestReport --console=plain --no-daemon -q
}
front_verify() {
  (cd "$ROOT/vite-react-teamsketch" && {
    case "$(command -v npm)" in /mnt/*) echo "Windows npm이 PATH에 있다. Linux node를 쓴다"; exit 1;; esac
    [ -d node_modules ] || npm ci --no-audit --no-fund
    npm run verify
  })
}
have_mysql_env() { [ -n "${HARU_TEST_MYSQL_URL:-}" ] && [ -n "${HARU_TEST_MYSQL_USER:-}" ] && [ -n "${HARU_TEST_MYSQL_PASSWORD+x}" ]; }
have_redis_env() { [ -n "${HARU_TEST_REDIS_HOST:-}" ] && [ -n "${HARU_TEST_REDIS_PORT:-}" ]; }
core_integration() { (cd "$ROOT/CoreService" && ./gradlew jacocoIntegrationReport --console=plain --no-daemon -q); }
sql_integration() { "$ROOT/CoreService/gradlew" -p "$ROOT/sql" mysqlIntegrationTest jacocoTestReport --console=plain --no-daemon -q; }

run_modules() { # run_modules <all|changed>
  local paths core=0 assist=0 gw=0 front=0 sql=0 e2e=0
  if [ "$1" = all ]; then core=1 assist=1 gw=1 front=1 sql=1 e2e=1
  else
    paths=$(changed_paths)
    grep -q '^CoreService/' <<<"$paths" && core=1
    grep -q '^sql/' <<<"$paths" && { sql=1; core=1; }
    grep -q '^AssistService/' <<<"$paths" && assist=1
    grep -q '^GateWay/' <<<"$paths" && gw=1
    grep -q '^vite-react-teamsketch/' <<<"$paths" && front=1
    grep -qE '^(compose\.yaml|api-tests/|contracts/|[^/]+/Dockerfile|scripts/gate/)' <<<"$paths" && e2e=1
    # 게이트 자체가 바뀌면 전 모듈을 다시 검증한다
    grep -q '^scripts/gate/' <<<"$paths" && { core=1 assist=1 gw=1 front=1 sql=1; }
  fi

  [ $core = 1 ] && step "CoreService check" gradle_check CoreService || skip "CoreService check" "변경 없음"
  [ $assist = 1 ] && step "AssistService check" gradle_check AssistService || skip "AssistService check" "변경 없음"
  [ $gw = 1 ] && step "GateWay check" gradle_check GateWay || skip "GateWay check" "변경 없음"
  [ $sql = 1 ] && step "sql test" sql_test || skip "sql test" "변경 없음"
  [ $front = 1 ] && step "frontend verify" front_verify || skip "frontend verify" "변경 없음"

  if [ $core = 1 ] || [ $sql = 1 ]; then
    if have_mysql_env && have_redis_env; then
      step "Core mysql+redis 통합" core_integration
      [ $sql = 1 ] && step "sql mysql 통합" sql_integration
    else
      # 이번에 돌지 않은 통합 레인의 지난 리포트가 diff 커버리지에 섞이면 미커버 줄이 커버로 보인다(H01.6 R7)
      rm -rf "$ROOT/CoreService/build/reports/jacoco/jacocoIntegrationReport" "$ROOT/sql/build/reports/jacoco/mysqlIntegrationTest"
      skip "통합 레인" "HARU_TEST_MYSQL_*/REDIS_* 없음 — CI가 실행(로컬: /haru-verify 참조). 지난 통합 리포트는 제외"
    fi
  fi
  E2E_NEEDED=$e2e
}

case "$MODE" in
  staged)
    step "policy(staged)" python3 "$GATE_DIR/policy.py" --staged
    ;;
  push|worktree)
    git rev-parse --verify -q "$BASE" >/dev/null || { echo "base '$BASE' 없음. git fetch origin 후 다시"; exit 2; }
    pflag=(--base "$BASE"); [ "$MODE" = worktree ] && pflag+=(--worktree)
    step "policy" python3 "$GATE_DIR/policy.py" "${pflag[@]}"
    if changed_paths | grep -q '^scripts/gate/'; then
      step "gate selftest" "$GATE_DIR/selftest/run.sh"
    fi
    step "git diff --check" bash -c "git diff --check \$(git merge-base '$BASE' HEAD) $( [ "$MODE" = push ] && echo HEAD ) -- . ':!*.md'"
    run_modules changed
    step "diff coverage" python3 "$GATE_DIR/diff_coverage.py" "${pflag[@]}"
    step "mutation (changed lines)" python3 "$GATE_DIR/mutation.py" "${pflag[@]}" --threshold "${GATE_MUTATION:-60}"
    if [ "${E2E_NEEDED:-0}" = 1 ]; then
      if [ "${GATE_E2E:-0}" = 1 ]; then step "e2e (compose+newman)" "$GATE_DIR/e2e.sh"
      else skip "e2e" "계약·compose 변경 감지 — CI가 실행(로컬: GATE_E2E=1)"; fi
    fi
    ;;
  full)
    git rev-parse --verify -q "$BASE" >/dev/null || { echo "base '$BASE' 없음"; exit 2; }
    step "policy" python3 "$GATE_DIR/policy.py" --base "$BASE" --worktree
    run_modules all
    step "diff coverage" python3 "$GATE_DIR/diff_coverage.py" --base "$BASE" --worktree
    step "mutation (changed lines)" python3 "$GATE_DIR/mutation.py" --base "$BASE" --worktree --threshold 60
    step "gate selftest" "$GATE_DIR/selftest/run.sh"
    if command -v docker >/dev/null && docker info >/dev/null 2>&1; then
      step "e2e (compose+newman)" "$GATE_DIR/e2e.sh"
    else
      skip "e2e" "docker 미가용"
      FAILED=1; RESULTS+=("FAIL e2e (full 모드는 docker 필수)")
    fi
    ;;
  e2e)
    step "e2e (compose+newman)" "$GATE_DIR/e2e.sh"
    ;;
  *)
    sed -n '2,12p' "$0"; exit 2 ;;
esac
summary
