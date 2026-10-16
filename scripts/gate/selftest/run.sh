#!/usr/bin/env bash
# ==============================================================================
# Haru 품질 게이트 자가 시험 (Sabotage Suite)
#
# 목적:
#   게이트 규칙(policy.py, diff_coverage.py, check/lint)이 실제 위반 코드를
#   정확히 차단하는지(True Positive)와 정상 코드를 통과시키는지(True Negative)를
#   독립된 임시 worktree에서 공격적으로 검증한다.
#
# 사용:
#   scripts/gate/selftest/run.sh [--slow] [-v|--verbose]
#
# 옵션:
#   --slow       느린 시나리오(diff 커버리지, Gradle check, npm lint) 포함
#   -v, --verbose 각 시나리오의 상세 출력 표시
# ==============================================================================

set -uo pipefail

SLOW_MODE=0
VERBOSE=0

for arg in "$@"; do
  case "$arg" in
    --slow) SLOW_MODE=1 ;;
    -v|--verbose) VERBOSE=1 ;;
    -h|--help)
      sed -n '2,15p' "$0"
      exit 0
      ;;
    *)
      echo "알 수 없는 옵션: $arg"
      echo "사용법: $0 [--slow] [-v|--verbose]"
      exit 2
      ;;
  esac
done

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

TEMP_WORKTREE=$(mktemp -d -t haru-gate-selftest-XXXXXX)
CLEANED=0

cleanup() {
  local exit_code=$?
  if [ "$CLEANED" -eq 1 ]; then
    exit "$exit_code"
  fi
  CLEANED=1
  echo ""
  echo "🧹 임시 worktree 정리 중: $TEMP_WORKTREE"
  cd "$REPO_ROOT"
  git worktree remove --force "$TEMP_WORKTREE" 2>/dev/null || true
  rm -rf "$TEMP_WORKTREE" 2>/dev/null || true
  exit "$exit_code"
}
trap cleanup EXIT INT TERM

echo "🚀 임시 worktree 생성 (HEAD 기준): $TEMP_WORKTREE"
git worktree add --detach "$TEMP_WORKTREE" HEAD >/dev/null 2>&1

# 게이트 검사 도구 및 게이트 설정 파일 선별 복사 (동시 작업자 변경사항 오염 방지)
cp -r "$REPO_ROOT/scripts" "$TEMP_WORKTREE/"

mkdir -p "$TEMP_WORKTREE/CoreService/src/test/resources"
if [ -d "$REPO_ROOT/CoreService/src/test/resources/archunit_store" ]; then
  cp -r "$REPO_ROOT/CoreService/src/test/resources/archunit_store" "$TEMP_WORKTREE/CoreService/src/test/resources/"
fi
if [ -f "$REPO_ROOT/CoreService/src/test/resources/archunit.properties" ]; then
  cp "$REPO_ROOT/CoreService/src/test/resources/archunit.properties" "$TEMP_WORKTREE/CoreService/src/test/resources/"
fi
if [ -d "$REPO_ROOT/CoreService/src/test/java/com/example/demo/architecture" ]; then
  mkdir -p "$TEMP_WORKTREE/CoreService/src/test/java/com/example/demo"
  cp -r "$REPO_ROOT/CoreService/src/test/java/com/example/demo/architecture" "$TEMP_WORKTREE/CoreService/src/test/java/com/example/demo/"
fi
if [ -f "$REPO_ROOT/vite-react-teamsketch/eslint-suppressions.json" ]; then
  cp "$REPO_ROOT/vite-react-teamsketch/eslint-suppressions.json" "$TEMP_WORKTREE/vite-react-teamsketch/"
fi
if [ -f "$REPO_ROOT/vite-react-teamsketch/eslint.config.js" ]; then
  cp "$REPO_ROOT/vite-react-teamsketch/eslint.config.js" "$TEMP_WORKTREE/vite-react-teamsketch/"
fi
if [ -f "$REPO_ROOT/vite-react-teamsketch/vitest.config.ts" ]; then
  cp "$REPO_ROOT/vite-react-teamsketch/vitest.config.ts" "$TEMP_WORKTREE/vite-react-teamsketch/"
fi

# build.gradle (jacoco threshold 검증용) 복사
cp "$REPO_ROOT/CoreService/build.gradle" "$TEMP_WORKTREE/CoreService/build.gradle"
cp "$REPO_ROOT/AssistService/build.gradle" "$TEMP_WORKTREE/AssistService/build.gradle"
cp "$REPO_ROOT/GateWay/build.gradle" "$TEMP_WORKTREE/GateWay/build.gradle"

# 프론트 node_modules 심볼릭 링크 연결 (재설치 시간 단축)
if [ -d "$REPO_ROOT/vite-react-teamsketch/node_modules" ]; then
  ln -s "$REPO_ROOT/vite-react-teamsketch/node_modules" "$TEMP_WORKTREE/vite-react-teamsketch/node_modules"
fi

cd "$TEMP_WORKTREE"

# 임시 worktree 안에서 게이트 기준 상태를 커밋(BASE_REF)으로 고정
git add -A >/dev/null 2>&1
# core.fileMode=false 저장소에서는 rsync로 복사한 실행 권한이 인덱스에 반영되지 않는다 → 원본 인덱스 모드를 그대로 복제
git -C "$REPO_ROOT" ls-files -s | awk '$1=="100755"{print $4}' | while read -r f; do
  [ -e "$f" ] && git update-index --chmod=+x -- "$f"
done
git commit -m "chore: base gate harness state for selftest" --no-verify >/dev/null 2>&1
BASE_REF=$(git rev-parse HEAD)

echo "📌 기준 커밋 고정: ${BASE_REF:0:10}"
echo ""

declare -a SCENARIO_NAMES=()
declare -a EXPECTED_RESULTS=()
declare -a ACTUAL_RESULTS=()
declare -a STATUS_RESULTS=()

TOTAL_COUNT=0
PASSED_COUNT=0
FAILED_COUNT=0
SKIPPED_COUNT=0
GAP_COUNT=0

reset_tree() {
  cd "$TEMP_WORKTREE"
  git reset --hard "$BASE_REF" >/dev/null 2>&1
  git clean -fd >/dev/null 2>&1
  unset GATE_APPROVED
}

# ------------------------------------------------------------------------------
# 시나리오 실행 헬퍼
# ------------------------------------------------------------------------------
# run_scenario <ID> <제목> <종류: FAIL|PASS|GAP|SKIP> <기대 규칙/설명> <실행 명령>
run_scenario() {
  local id="$1"
  local name="$2"
  local kind="$3"
  local expected="$4"
  shift 4
  local cmd=("$@")

  TOTAL_COUNT=$((TOTAL_COUNT + 1))
  local display_name="[$id] $name"
  SCENARIO_NAMES+=("$display_name")
  EXPECTED_RESULTS+=("$expected")

  if [ "$kind" = "SKIP" ]; then
    ACTUAL_RESULTS+=("SKIP ($expected)")
    STATUS_RESULTS+=("SKIP")
    SKIPPED_COUNT=$((SKIPPED_COUNT + 1))
    printf "  ▷ %-50s SKIP — %s\n" "$display_name" "$expected"
    return 0
  fi

  reset_tree

  # 시나리오 준비 및 검증 커맨드 실행
  local log_output
  local exit_code=0
  log_output=$("${cmd[@]}" 2>&1) || exit_code=$?

  local actual_summary=""
  local status=""

  if [ "$kind" = "FAIL" ]; then
    if [ "$exit_code" -ne 0 ] && grep -qF "$expected" <<<"$log_output"; then
      actual_summary="차단됨 ($expected, exit $exit_code)"
      status="PASS"
      PASSED_COUNT=$((PASSED_COUNT + 1))
    elif [ "$exit_code" -eq 0 ]; then
      actual_summary="실패: 예상과 달리 통과(exit 0)"
      status="FAIL"
      FAILED_COUNT=$((FAILED_COUNT + 1))
    else
      local first_err
      first_err=$(grep -E '✗ \[[^]]+\]' <<<"$log_output" | head -n 1 | sed 's/^[[:space:]]*//')
      actual_summary="규칙 불일치: ${first_err:-exit $exit_code}"
      status="FAIL"
      FAILED_COUNT=$((FAILED_COUNT + 1))
    fi
  elif [ "$kind" = "PASS" ]; then
    if [ "$exit_code" -eq 0 ]; then
      actual_summary="통과 (exit 0)"
      status="PASS"
      PASSED_COUNT=$((PASSED_COUNT + 1))
    else
      local first_err
      first_err=$(grep -E '✗ \[[^]]+\]' <<<"$log_output" | head -n 1 | sed 's/^[[:space:]]*//')
      actual_summary="오탐지 차단: ${first_err:-exit $exit_code}"
      status="FAIL"
      FAILED_COUNT=$((FAILED_COUNT + 1))
    fi
  elif [ "$kind" = "GAP" ]; then
    # 구멍 탐지: 나쁜 변경인데도 게이트가 통과(exit 0)하거나 해당 규칙으로 차단하지 못하는 현상
    if [ "$exit_code" -eq 0 ] || ! grep -qF "$expected" <<<"$log_output"; then
      actual_summary="구멍 확인됨 (게이트 우회/미탐지)"
      status="PASS(GAP)"
      PASSED_COUNT=$((PASSED_COUNT + 1))
      GAP_COUNT=$((GAP_COUNT + 1))
    else
      actual_summary="게이트가 차단함 (구멍 패치됨)"
      status="PASS"
      PASSED_COUNT=$((PASSED_COUNT + 1))
    fi
  fi

  ACTUAL_RESULTS+=("$actual_summary")
  STATUS_RESULTS+=("$status")

  if [ "$status" = "FAIL" ]; then
    printf "  ✗ %-50s %s (%s)\n" "$display_name" "$status" "$actual_summary"
    if [ "$VERBOSE" -eq 1 ]; then
      echo "    --- 로그 시작 ---"
      tail -n 15 <<<"$log_output" | sed 's/^/    | /'
      echo "    --- 로그 끝 ---"
    fi
  elif [ "$status" = "PASS(GAP)" ]; then
    printf "  ! %-50s %s (%s)\n" "$display_name" "$status" "$actual_summary"
  else
    printf "  ✓ %-50s %s\n" "$display_name" "$status"
  fi
}

echo "=============================================================================="
echo " 1. 빠른 시나리오 (Policy 검사 — worktree / staged)"
echo "=============================================================================="

# S00: 양성 대조군 - 아무것도 바꾸지 않은 상태
run_scenario "S00" "양성 대조: 깨끗한 상태" "PASS" "exit 0" \
  bash -c 'python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree'

# S01: Java main에 System.out.println -> java-stdout
run_scenario "S01" "Java main에 System.out.println" "FAIL" "java-stdout" \
  bash -c '
    sed -i "/public static void main/a \\        System.out.println(\"sabotage\");" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S02: Java main에 e.printStackTrace() -> java-stdout
run_scenario "S02" "Java main에 e.printStackTrace()" "FAIL" "java-stdout" \
  bash -c '
    sed -i "/public static void main/a \\        new RuntimeException().printStackTrace();" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S03: Core main(TokenUtils 외)에 "Bearer " 문자열 -> token-single-source
run_scenario "S03" "Core main에 Bearer 리터럴" "FAIL" "token-single-source" \
  bash -c '
    sed -i "/public static void main/a \\        String auth = \"Bearer sabotage\";" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S04a: Java main에 "http://localhost:8081" 리터럴 -> hardcoded-host
run_scenario "S04a" "Java main에 localhost 리터럴" "FAIL" "hardcoded-host" \
  bash -c '
    sed -i "/public static void main/a \\        String host = \"http://localhost:8081/api\";" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S04b: Java main에 @Value("${x:http://localhost}") -> 양성 대조군 통과
run_scenario "S04b" "양성 대조: @Value(\"\${x:http://localhost}\")" "PASS" "exit 0" \
  bash -c '
    sed -i "/public class Application/a \\    @org.springframework.beans.factory.annotation.Value(\"\${x:http://localhost:8081}\") private String host;" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S05: 프론트 src/ 비테스트 파일에 console.log -> ts-console
run_scenario "S05" "프론트 src에 console.log" "FAIL" "ts-console" \
  bash -c '
    echo "console.log(\"sabotage\");" >> vite-react-teamsketch/src/main.tsx
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S06a: Java 테스트에 @Disabled -> test-disabled
run_scenario "S06a" "Java 테스트에 @Disabled" "FAIL" "test-disabled" \
  bash -c '
    TARGET=$(find CoreService/src/test/java -name "*Test*.java" | head -n 1)
    sed -i "/@Test/i \\    @Disabled" "$TARGET"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S06b: 프론트 테스트에 it.only -> test-disabled
run_scenario "S06b" "프론트 테스트에 it.only" "FAIL" "test-disabled" \
  bash -c '
    mkdir -p vite-react-teamsketch/src/test
    echo "it.only(\"sabotage test\", () => {});" >> vite-react-teamsketch/src/test/dummy.test.ts
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S07: 기존 테스트 파일에서 @Test 메서드 하나 삭제 -> test-count-decreased
run_scenario "S07" "기존 테스트 @Test 메서드 삭제" "FAIL" "test-count-decreased" \
  bash -c '
    TARGET=$(grep -l "@Test" $(find CoreService/src/test/java -name "*Test*.java") | head -n 1)
    sed -i "0,/@Test/s//@CommentedOutTest/" "$TARGET"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S08: 테스트 파일 하나를 이름만 바꿔 이동 -> 양성 대조군(순감 0) 통과
run_scenario "S08" "양성 대조: 테스트 파일 rename (순감 0)" "PASS" "exit 0" \
  bash -c '
    SRC="CoreService/src/test/java/com/example/demo/util/TokenUtilsTest.java"
    DST="CoreService/src/test/java/com/example/demo/util/TokenUtilsRenamedTest.java"
    git mv "$SRC" "$DST"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S09: sql/migrations/V1__*.sql 한 줄 수정 -> migration-immutable
run_scenario "S09" "기존 마이그레이션 V1 수정" "FAIL" "migration-immutable" \
  bash -c '
    TARGET=$(find sql/migrations -name "V1__*.sql" | head -n 1)
    echo "-- sabotage modification" >> "$TARGET"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S10: sql/schema.sql 수정 -> migration-baseline
run_scenario "S10" "기준선 sql/schema.sql 수정" "FAIL" "migration-baseline" \
  bash -c '
    echo "-- sabotage baseline change" >> sql/schema.sql
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S11a: V9__x.sql 추가(버전 건너뛰기) -> migration-version
run_scenario "S11a" "마이그레이션 버전 건너뛰기(V9__x.sql)" "FAIL" "migration-version" \
  bash -c '
    echo "SELECT 1;" > sql/migrations/V9__skip_version.sql
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S11b: v4_bad.sql 추가(이름 규칙 위반) -> migration-name
run_scenario "S11b" "마이그레이션 이름 규칙 위반(v4_bad.sql)" "FAIL" "migration-name" \
  bash -c '
    echo "SELECT 1;" > sql/migrations/v4_bad_name.sql
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S11c: 유효한 다음 버전 마이그레이션 추가 -> 양성 대조군 통과
run_scenario "S11c" "양성 대조: 정상 다음 마이그레이션 추가" "PASS" "exit 0" \
  bash -c '
    LAST_VER=$(ls sql/migrations/V*__*.sql | sed -E "s/.*V([0-9]+)__.*/\1/" | sort -n | tail -n 1)
    NEXT_VER=$((LAST_VER + 1))
    echo "SELECT 1;" > "sql/migrations/V${NEXT_VER}__valid_next_migration.sql"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S12a: CoreService application.properties에 password=RealPass123 -> secret
run_scenario "S12a" "설정 파일 시크릿 하드코딩" "FAIL" "secret" \
  bash -c '
    echo "spring.datasource.password=RealPass123" >> CoreService/src/main/resources/application.properties
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S12b: application.properties에 password=${DB_PASSWORD} -> 양성 대조군 통과
run_scenario "S12b" "양성 대조: 설정 파일 환경변수 치환문" "PASS" "exit 0" \
  bash -c '
    echo "spring.datasource.password=\${DB_PASSWORD}" >> CoreService/src/main/resources/application.properties
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S13: .env 파일 추가 -> secret-file (git add -f로 스테이징)
run_scenario "S13" "비밀 파일(.env) 추가" "FAIL" "secret-file" \
  bash -c '
    echo "SOME_SECRET=abcd" > .env
    git add -f .env
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S14: 텍스트에 가짜 AWS 키 패턴 -> secret
run_scenario "S14" "가짜 AWS 키 패턴(AKIA...)" "FAIL" "secret" \
  bash -c '
    # 키 모양 문자열을 소스에 그대로 두면 이 파일 자체가 시크릿 검사(policy·gitleaks)에 걸린다 → 실행 시 조립
    printf "aws_key = %s%s\n" "AKIA" "IOSFODNN7EXAMPLE" > aws_credentials.txt
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S15: 2MB 파일 추가 -> large-file
run_scenario "S15" "1MiB 초과 대용량 파일 추가" "FAIL" "large-file" \
  bash -c '
    head -c 2097152 /dev/zero > large_blob.dat
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S16a: scripts/gate/policy.py 수정 -> gate-protected
run_scenario "S16a" "게이트 파일(policy.py) 무단 수정" "FAIL" "gate-protected" \
  bash -c '
    echo "# sabotage comment" >> scripts/gate/policy.py
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S16b: policy.py 수정 후 GATE_APPROVED=1 -> 양성 대조군 통과(경고)
run_scenario "S16b" "양성 대조: GATE_APPROVED=1로 게이트 수정" "PASS" "exit 0" \
  bash -c '
    echo "# sabotage comment" >> scripts/gate/policy.py
    GATE_APPROVED=1 python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S17: archunit_store 파일에 줄 추가 -> archunit-freeze-grew
ARCHUNIT_SAMPLE=$(find CoreService/src/test/resources/archunit_store -type f ! -name "stored.rules" 2>/dev/null | head -n 1 || true)
if [ -n "$ARCHUNIT_SAMPLE" ]; then
  run_scenario "S17" "ArchUnit 동결 위반 증가" "FAIL" "archunit-freeze-grew" \
    bash -c '
      echo "com.example.demo.Sabotage.rule" >> "'"$ARCHUNIT_SAMPLE"'"
      python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
    '
else
  run_scenario "S17" "ArchUnit 동결 위반 증가" "SKIP" "archunit_store 디렉토리/파일 없음"
fi

# S18: eslint-suppressions.json count 증가 -> eslint-suppression-grew
if [ -f "vite-react-teamsketch/eslint-suppressions.json" ]; then
  run_scenario "S18" "ESLint 동결 위반 수 증가" "FAIL" "eslint-suppression-grew" \
    bash -c '
      python3 -c "
import json
p = \"vite-react-teamsketch/eslint-suppressions.json\"
d = json.load(open(p))
for file_rules in d.values():
    for rule, val in file_rules.items():
        if isinstance(val, dict) and \"count\" in val:
            val[\"count\"] += 1
            break
        elif isinstance(val, int):
            file_rules[rule] = val + 1
            break
    break
json.dump(d, open(p, \"w\"), indent=2)
"
      python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
    '
else
  run_scenario "S18" "ESLint 동결 위반 수 증가" "SKIP" "eslint-suppressions.json 파일 없음"
fi

# S19: build.gradle의 JaCoCo minimum 값 낮추기 -> coverage-threshold-lowered
run_scenario "S19" "커버리지 하한선 하향(minimum 값 감소)" "FAIL" "coverage-threshold-lowered" \
  bash -c '
    sed -i "s/minimum = .*/minimum = 0.05/" CoreService/build.gradle
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S20: build.gradle에 ignoreFailures = true -> gradle-gate-config 또는 gate-bypass
run_scenario "S20" "빌드 스크립트에 ignoreFailures=true 삽입" "FAIL" "gate-protected" \
  bash -c '
    echo "test { ignoreFailures = true }" >> CoreService/build.gradle
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# S21: 충돌 표식 <<<<<<< HEAD -> conflict-marker
run_scenario "S21" "머지 충돌 표식(<<<<<<< HEAD)" "FAIL" "conflict-marker" \
  bash -c '
    echo "<<<<<<< HEAD" > conflict_test.txt
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

echo ""
echo "=============================================================================="
echo " 2. 구멍 탐지 시나리오 (EXPECT_GAP — 게이트 우회 취약점 발굴)"
echo "=============================================================================="

# G01: System .out.println (공백 삽입) -> 정규식 우회
run_scenario "G01" "System .out.println 공백 삽입 우회" "FAIL" "java-stdout" \
  bash -c '
    sed -i "/public static void main/a \\        System .out.println(\"gap-stdout\");" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# G02: "Bear" + "er " 문자열 분할 -> token-single-source 우회
run_scenario "G02" "Bearer 문자열 연결 분할 우회" "GAP" "token-single-source" \
  bash -c '
    sed -i "/public static void main/a \\        String auth = \"Bear\" + \"er token\";" CoreService/src/main/java/com/example/demo/Application.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# G03: @Test 내 단언(assert)만 삭제 (메서드 선언 유지) -> test-count-decreased 우회
run_scenario "G03" "단언문(assert) 삭제 후 빈 테스트 유지" "FAIL" "assertion-count-decreased" \
  bash -c '
    local_target=$(grep -lE "assert(Equals|That|True)\(" $(find CoreService/src/test/java -name "*Test.java") | head -n 1)
    python3 -c "
import re
text = open(\"${local_target}\").read()
text = re.sub(r\"assert\w*\(.*?\);\", \"// assert removed\", text)
open(\"${local_target}\", \"w\").write(text)
"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# G04: .env.local 파일 추가 -> secret-file 우회 (SECRET_FILE_NAMES 정규식 한계)
run_scenario "G04" ".env.local 파일 추가 우회" "FAIL" "secret-file" \
  bash -c '
    echo "LOCAL_CONFIG=value" > .env.local
    git add -f .env.local   # .gitignore(*.local)를 무시하고 강제로 스테이징하는 실제 유출 경로
    python3 scripts/gate/policy.py --staged
  '

# G05: console.table(["gap"]) -> ts-console 우회
run_scenario "G05" "console.table 호출 우회" "FAIL" "ts-console" \
  bash -c '
    echo "console.table([\"gap\"]);" >> vite-react-teamsketch/src/main.tsx
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# G06: 들여쓰기된 충돌 표식 (   <<<<<<< HEAD) -> conflict-marker 우회
run_scenario "G06" "들여쓰기된 머지 충돌 표식 우회" "FAIL" "conflict-marker" \
  bash -c '
    echo "   <<<<<<< HEAD" > conflict_indented.txt
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# G07: 하위 디렉토리 내 마이그레이션 (sql/migrations/subdir/V4__x.sql) -> migration 규칙 우회
run_scenario "G07" "하위 폴더 내 마이그레이션 파일 우회" "FAIL" "migration-name" \
  bash -c '
    mkdir -p sql/migrations/subdir
    echo "SELECT 1;" > sql/migrations/subdir/V99__deep.sql
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# G08: FQCN @org.junit.jupiter.api.Disabled -> test-disabled 우회
run_scenario "G08" "FQCN @org.junit...Disabled 사용 우회" "FAIL" "test-disabled" \
  bash -c '
    local_target=$(find CoreService/src/test/java -name "*Test*.java" | head -n 1)
    sed -i "/@Test/i \\    @org.junit.jupiter.api.Disabled" "${local_target}"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '


echo ""
echo "=============================================================================="
echo " 2b. H01.6 적대 리뷰(codex) 재현 사례 — 고친 구멍이 다시 열리지 않게 고정"
echo "=============================================================================="

# R1a: ArchUnit 규칙 본문을 비워도 테스트 수는 그대로 → 규칙 파일 자체를 보호
run_scenario "H01" "ArchUnit 규칙 본문 제거" "FAIL" "gate-protected" \
  bash -c '
    sed -i "s/NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(CLASSES);/;/" CoreService/src/test/java/com/example/demo/architecture/ArchitectureRulesTest.java
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R1b: 검증 태스크 action 제거(토큰 목록에 없는 표현) → build.gradle 파일 단위 보호
run_scenario "H02" "Gradle 검증 태스크 action 제거" "FAIL" "gate-protected" \
  bash -c '
    echo "tasks.matching { it.name.endsWith(\"CoverageVerification\") }.configureEach { actions.clear() }" >> CoreService/build.gradle
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R2a: 내용 변경 없는 마이그레이션 이름 변경(패치 본문 없음)
run_scenario "H03" "마이그레이션 순수 이름 변경(staged)" "FAIL" "migration-immutable" \
  bash -c '
    f=$(ls sql/migrations/V1__*.sql | head -n 1)
    git mv "$f" sql/migrations/V1__renamed.sql
    python3 scripts/gate/policy.py --staged
  '

# R2b: 바이너리 2MiB 스테이징(패치 본문 없음)
run_scenario "H04" "바이너리 2MiB 파일 스테이징" "FAIL" "large-file" \
  bash -c '
    head -c 2097152 /dev/zero > h04-binary.dat
    git add h04-binary.dat
    python3 scripts/gate/policy.py --staged
  '

# R3: 프로덕션이 테스트 제외 경로를 import해 업무 코드를 커버리지 밖에 숨김
run_scenario "H05" "프로덕션이 src/test 경로 import" "FAIL" "prod-imports-test" \
  bash -c '
    mkdir -p vite-react-teamsketch/src/test
    echo "export const price = (t: number) => t * -100;" > vite-react-teamsketch/src/test/h05-business.ts
    echo "import \"./test/h05-business\";" >> vite-react-teamsketch/src/main.tsx
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R5a: ESLint 동결 총량은 같게 두고 다른 파일로 교체
run_scenario "H06" "ESLint 동결 교체(총량 동일)" "FAIL" "eslint-suppression-grew" \
  bash -c '
    python3 - <<PY
import json
p = "vite-react-teamsketch/eslint-suppressions.json"
d = json.load(open(p))
f = next(iter(d)); r = next(iter(d[f]))
d[f][r]["count"] -= 1
if d[f][r]["count"] == 0:
    del d[f][r]
d.setdefault("src/h06-new-debt.ts", {})["@typescript-eslint/no-explicit-any"] = {"count": 1}
json.dump(d, open(p, "w"), indent=2)
PY
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R5b: ArchUnit 동결 위반 한 줄을 다른 위반으로 교체(줄 수 동일)
run_scenario "H07" "ArchUnit 동결 위반 교체(줄 수 동일)" "FAIL" "archunit-freeze-grew" \
  bash -c '
    f=$(grep -l "controller" CoreService/src/test/resources/archunit_store/* | grep -v stored.rules | head -n 1)
    sed -i "1s/.*/Class <com.example.demo.controller.H07Controller> depends on <com.example.demo.mapper.H07Mapper> in (H07Controller.java:0)/" "$f"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R6a: FQCN 조건부 비활성화
run_scenario "H08" "FQCN @DisabledIfSystemProperty" "FAIL" "test-conditional-skip" \
  bash -c '
    t=CoreService/src/test/java/com/example/demo/util/TokenUtilsTest.java
    sed -i "0,/@Test/s//@org.junit.jupiter.api.condition.DisabledIfSystemProperty(named = \"java.version\", matches = \".*\")\n    @Test/" "$t"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R6b: 단언을 주석으로 남겨 개수 유지
run_scenario "H09" "단언을 주석 처리" "FAIL" "assertion-count-decreased" \
  bash -c '
    t=$(grep -lE "^\s*assert(Equals|That|True)\(" $(find CoreService/src/test/java -name "*Test.java") | head -n 1)
    sed -i "0,/^\(\s*\)\(assert\(Equals\|That\|True\)(\)/s//\1\/\/ \2/" "$t"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# R10 양성 대조: @Test → @org.junit.jupiter.api.Test 는 동작 동일, 통과해야 함
run_scenario "H10" "양성 대조: @Test FQCN 전환" "PASS" "exit 0" \
  bash -c '
    t=CoreService/src/test/java/com/example/demo/util/TokenUtilsTest.java
    sed -i "0,/^\(\s*\)@Test$/s//\1@org.junit.jupiter.api.Test/" "$t"
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# 양성 대조: 한글 경로 문서 추가(따옴표 경로 파싱)
run_scenario "H11" "양성 대조: 한글 경로 파일 추가" "PASS" "exit 0" \
  bash -c '
    mkdir -p "docs/검증" && echo "메모" > "docs/검증/한글 파일.txt"
    git add -f "docs/검증/한글 파일.txt"
    python3 scripts/gate/policy.py --staged
  '


# ---- H01.7 재리뷰(codex) 재현 사례 ----
# N1-A: main.tsx import 줄 뒤에 실행문을 붙임 → 진입점 변경 줄은 미커버로 센다
run_scenario "H12" "main.tsx import 줄 뒤 실행문" "FAIL" "변경 줄 커버리지" \
  bash -c '
    echo "import \"./assets/styles/index.css\"; document.title = \"h12\" + (100 * -100);" >> vite-react-teamsketch/src/main.tsx
    python3 scripts/gate/diff_coverage.py --base "'"$BASE_REF"'" --worktree --only front
  '

# N1-B: 주석을 끼운 동적 import로 테스트 경로를 가져옴
run_scenario "H13" "주석 낀 동적 import로 test 경로" "FAIL" "prod-imports-test" \
  bash -c '
    mkdir -p vite-react-teamsketch/src/test
    echo "export const p = 1;" > vite-react-teamsketch/src/test/h13.ts
    echo "import (/* lazy */ \"./test/h13\");" >> vite-react-teamsketch/src/main.tsx
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# N2: 공백이 든 파일명(헤더 끝 탭)에서도 줄 검사가 정확한 경로에 붙어야 한다
run_scenario "H14" "공백 파일명 TS에 console.table(staged)" "FAIL" "ts-console" \
  bash -c '
    printf "export const x = 1;\nconsole.table([x]);\n" > "vite-react-teamsketch/src/h14 space.ts"
    git add "vite-react-teamsketch/src/h14 space.ts"
    python3 scripts/gate/policy.py --staged
  '

# N3 양성 대조: 문자열 안의 // 는 주석이 아니다(단언 수 그대로)
run_scenario "H15" "양성 대조: 문자열 \"//users\" 단언" "PASS" "exit 0" \
  bash -c '
    t=CoreService/src/test/java/com/example/demo/util/TokenUtilsTest.java
    python3 - "$t" <<PY
import re, sys
p = sys.argv[1]; s = open(p).read()
s = s.replace("@Test", "@Test\n    void h15() { String r = \"/users\"; org.assertj.core.api.Assertions.assertThat(r).isEqualTo(\"/users\"); }\n\n    @Test", 1)
open(p, "w").write(s)
PY
    git add "$t" && git commit -qm h15-base --no-verify
    sed -i "s#\"/users\"#\"//users\"#g" "$t"
    python3 scripts/gate/policy.py --base HEAD --worktree
  '


# ---- H01.8 재확인(codex) 재현 사례 ----
# F1: 인라인 블록 주석 뒤 실행문
run_scenario "H16" "main.tsx 주석 뒤 실행문" "FAIL" "변경 줄 커버리지" \
  bash -c '
    echo "/* entry note */ document.title = \"h16\" + (100 * -100);" >> vite-react-teamsketch/src/main.tsx
    python3 scripts/gate/diff_coverage.py --base "'"$BASE_REF"'" --worktree --only front
  '

# F2: 문자열 안 주석 기호 사이의 실제 import
run_scenario "H17" "문자열 \"/*\" \"*/\" 사이의 test import" "FAIL" "prod-imports-test" \
  bash -c '
    mkdir -p vite-react-teamsketch/src/test
    echo "export const p = 1;" > vite-react-teamsketch/src/test/h17.ts
    printf "const open = %s;\nimport(%s);\nconst close = %s;\nexport { open, close };\n" "\"/*\"" "\"./test/h17\"" "\"*/\"" > vite-react-teamsketch/src/h17-import.ts
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# F2 양성 대조: 문자열 안 import 예문은 코드가 아니다
run_scenario "H18" "양성 대조: 문자열 속 import 예문" "PASS" "exit 0" \
  bash -c '
    printf "export const help = \`Example: import %s;\`;\n" "\"./test/demo\"" > vite-react-teamsketch/src/h18-help.ts
    python3 scripts/gate/policy.py --base "'"$BASE_REF"'" --worktree
  '

# F3: 템플릿 보간 안의 실제 단언 삭제
run_scenario "H19" "템플릿 \${} 안의 단언 삭제" "FAIL" "assertion-count-decreased" \
  bash -c '
    t=vite-react-teamsketch/src/h19.test.ts
    printf "test(\"v\", () => {\n  const r = \`\${expect(1).toBe(1)}\`;\n  expect(r).toBe(\"undefined\");\n});\n" > "$t"
    git add "$t" && git commit -qm h19-base --no-verify
    sed -i "s/\`\${expect(1).toBe(1)}\`/\`undefined\`/" "$t"
    python3 scripts/gate/policy.py --base HEAD --worktree
  '


# H01.9 A1: Java 텍스트 블록 안의 "/*" 뒤 실제 테스트 삭제
run_scenario "H20" "텍스트 블록 \"/*\" 뒤 테스트 삭제" "FAIL" "test-count-decreased" \
  bash -c '
    t=CoreService/src/test/java/com/example/demo/util/H20Test.java
    printf "package com.example.demo.util;\nclass H20Test {\n  String body = \"\"\"\n    {\"path\": \"/api/*\"}\n    \"\"\";\n  @org.junit.jupiter.api.Test void t() { org.junit.jupiter.api.Assertions.assertEquals(1, 1); }\n}\n" > "$t"
    git add "$t" && git commit -qm h20-base --no-verify
    sed -i "/void t()/d" "$t"
    python3 scripts/gate/policy.py --base HEAD --worktree
  '


# PR #10 CI에서 실제 발생: 실행 비트 없이 커밋된 게이트 스크립트·훅
run_scenario "H21" "게이트 스크립트 실행 비트 제거(staged)" "FAIL" "exec-bit" \
  bash -c '
    printf "\n# h21\n" >> scripts/gate/e2e.sh
    git add scripts/gate/e2e.sh && git update-index --chmod=-x scripts/gate/e2e.sh
    python3 scripts/gate/policy.py --staged
  '

if [ "$SLOW_MODE" -eq 1 ]; then
  echo ""
  echo "=============================================================================="
  echo " 3. 느린 시나리오 (--slow 모드: Gradle / Diff Coverage / ESLint)"
  echo "=============================================================================="

  # S22: Core 서비스 미커버 줄 변경 -> diff_coverage.py 실패
  run_scenario "S22" "테스트 미실행 줄 변경 시 diff 커버리지 실패" "FAIL" "80" \
    bash -c '
      sed -i "/public Integer getUserIdFromToken/a \\        int uncovered_line = 9999;" CoreService/src/main/java/com/example/demo/util/TokenUtils.java
      (cd CoreService && ./gradlew test jacocoTestReport --console=plain --no-daemon -q)
      python3 scripts/gate/diff_coverage.py --base "'"$BASE_REF"'" --worktree --only java
    '

  # S23: Core 서비스 커버 줄 변경 -> diff_coverage.py 통과 (양성 대조군)
  run_scenario "S23" "양성 대조: 테스트 실행 줄 변경 시 diff 커버리지 통과" "PASS" "exit 0" \
    bash -c '
      sed -i "/public String extractTokenWithoutBearer/a \\        int covered_line = 1;" CoreService/src/main/java/com/example/demo/util/TokenUtils.java
      (cd CoreService && ./gradlew test jacocoTestReport --console=plain --no-daemon -q)
      python3 scripts/gate/diff_coverage.py --base "'"$BASE_REF"'" --worktree --only java
    '

  # S24: Core에 System.out.println 추가 -> gradlew check (ArchUnit) 실패
  run_scenario "S24" "ArchUnit: System.out 추가 시 check/test 실패" "FAIL" "FAILED" \
    bash -c '
      sed -i "/public static void main/a \\        System.out.println(\"archunit-fail\");" CoreService/src/main/java/com/example/demo/Application.java
      (cd CoreService && ./gradlew test --tests ArchitectureRulesTest --console=plain --no-daemon)
    '

  # S25: 프론트 console.log 추가 -> npm run lint 실패
  run_scenario "S25" "ESLint: console.log 추가 시 npm run lint 실패" "FAIL" "Unexpected console statement" \
    bash -c "
      echo 'console.log(\"eslint-fail\");' >> vite-react-teamsketch/src/main.tsx
      (cd vite-react-teamsketch && npm run lint)
    "
fi

# ------------------------------------------------------------------------------
# 결과 표 및 최종 판정 출력
# ------------------------------------------------------------------------------
echo ""
echo "=============================================================================="
echo " 자가 시험 (Sabotage Suite) 결과 요약표"
echo "=============================================================================="
printf "%-54s | %-28s | %-32s | %s\n" "시나리오" "기대" "실제" "결과"
echo "-------------------------------------------------------+------------------------------+----------------------------------+----------"

for i in "${!SCENARIO_NAMES[@]}"; do
  printf "%-54s | %-28s | %-32s | %s\n" \
    "${SCENARIO_NAMES[$i]}" "${EXPECTED_RESULTS[$i]}" "${ACTUAL_RESULTS[$i]}" "${STATUS_RESULTS[$i]}"
done

echo "-------------------------------------------------------+------------------------------+----------------------------------+----------"
echo "총 시나리오: $TOTAL_COUNT (통과: $PASSED_COUNT, 실패: $FAILED_COUNT, 스킵: $SKIPPED_COUNT, 구멍(GAP): $GAP_COUNT)"
echo ""
echo "selftest: ${PASSED_COUNT}/${TOTAL_COUNT} passed"

if [ "$FAILED_COUNT" -ne 0 ]; then
  echo "✗ 자가 시험 실패 (기대와 다른 시나리오 존재)"
  exit 1
fi

echo "✓ 모든 자가 시험 시나리오가 기대한 대로 동작함"
exit 0
