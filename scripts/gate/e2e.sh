#!/usr/bin/env bash
# compose 전 스택(빈 DB) + Newman 백엔드 회귀. 로컬 개발 스택·볼륨·.env를 건드리지 않는다.
#   - 프로젝트 이름/볼륨: haru-e2e-<id> (종료 시 down -v 로 제거)
#   - 시크릿: 매 실행 무작위 생성, 임시 env 파일에만 존재
#   - 포트: E2E_GATEWAY_PORT(기본 18080) — 개발 스택 8080과 충돌하지 않는다
# 결과: build/gate/newman-junit.xml
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
ID=${GITHUB_RUN_ID:-$$}
PROJECT="haru-e2e-$ID"
PORT=${E2E_GATEWAY_PORT:-18080}
OUT="$ROOT/build/gate"
mkdir -p "$OUT"
ENVF=$(mktemp)
rnd() { od -An -N"$1" -tx1 /dev/urandom | tr -d ' \n'; }

cat >"$ENVF" <<EOF
COMPOSE_PROJECT_NAME=$PROJECT
TZ=Asia/Seoul
MYSQL_ROOT_PASSWORD=$(rnd 16)
REDIS_PASSWORD=$(rnd 16)
JWT_SECRET=$(rnd 32)
MYSQL_DATABASE=haru_db
JWT_EXPIRATION=86400000
GATEWAY_PORT=$PORT
FRONTEND_PORT=$((PORT + 1))
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/redirect
EOF

dc() { docker compose --env-file "$ENVF" -p "$PROJECT" "$@"; }
cleanup() {
  local rc=$?
  if [ $rc -ne 0 ]; then
    echo "── e2e 실패: 서비스 로그(마지막 150줄)"
    dc ps -a || true
    dc logs --no-color --tail 150 migrator core assist gateway 2>&1 | tail -n 600 || true
  fi
  [ "${E2E_KEEP:-0}" = 1 ] || dc down -v --remove-orphans >/dev/null 2>&1 || true
  rm -f "$ENVF"
  exit $rc
}
trap cleanup EXIT

BASE_URL="http://127.0.0.1:$PORT"
echo "[e2e] build+up $PROJECT (gateway :$PORT)"
dc up -d --build mysql redis migrator core assist gateway

echo "[e2e] 게이트웨이→Core→DB 왕복 대기"
deadline=$((SECONDS + ${E2E_TIMEOUT:-600}))
until code=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' \
        "$BASE_URL/api/core/auth/login"); [ "$code" = 400 ]; do
  if [ $SECONDS -ge $deadline ]; then echo "[e2e] 시간 초과(마지막 HTTP $code)"; exit 1; fi
  if dc ps -a --format '{{.Service}} {{.State}} {{.ExitCode}}' | awk '$1=="migrator" && $2=="exited" && $3!=0 {f=1} END{exit !f}'; then
    echo "[e2e] migrator 실패"; exit 1
  fi
  sleep 3
done
# Assist도 준비돼야 02 폴더의 Assist 검사가 의미가 있다
until [ "$(dc ps --format '{{.Service}} {{.Health}}' | awk '$1=="assist"{print $2}')" = healthy ]; do
  [ $SECONDS -ge $deadline ] && { echo "[e2e] assist unhealthy"; exit 1; }
  sleep 3
done

EMAIL="e2e-$(rnd 4)@haru.test"
PASS="E2e!$(rnd 6)"
echo "[e2e] 전용 계정 가입 $EMAIL"
code=$(curl -s -o "$OUT/signup.json" -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
  -d "{\"name\":\"E2E\",\"email\":\"$EMAIL\",\"nickname\":\"e2e$(rnd 3)\",\"password\":\"$PASS\"}" \
  "$BASE_URL/api/core/auth/signup")
case "$code" in 200|201) ;; *) echo "[e2e] 가입 실패 HTTP $code"; cat "$OUT/signup.json"; exit 1;; esac

echo "[e2e] newman 4폴더"
npx --yes newman@6.2.3 run api-tests/haru.postman_collection.json \
  --folder "01 Startup" --folder "02 Authenticated" --folder "03 Route auth" --folder "04 Board CRUD" \
  --env-var "baseUrl=$BASE_URL" --env-var "email=$EMAIL" --env-var "password=$PASS" --env-var "enableCrud=true" \
  --timeout-request 10000 --bail failure \
  --reporters cli,junit,json --reporter-junit-export "$OUT/newman-junit.xml" --reporter-json-export "$OUT/newman.json"

# JUnit은 건너뛴 단언을 통과로 센다(H01.6 R4) → JSON으로 실행된 요청·단언·skipped를 직접 확인
python3 "$ROOT/scripts/gate/newman_check.py" "$OUT/newman.json"
echo "[e2e] ✓"
