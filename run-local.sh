#!/usr/bin/env bash
# =============================================================================
# Haru 로컬 개발 구동 스크립트 (Windows 11 / Git Bash)
#
#   사용법:  ./run-local.sh
#
#   동작:
#     1) 인프라 컨테이너(haru-redis 6379 / haru-mysql 3307)를 확인하고 없으면 start
#     2) GateWay(8080) → CoreService(8081) → AssistService(8082) 3개 Spring
#        서비스를 순차 기동한다. 로그는 logs/<서비스>.log 에 쌓인다.
#        (동시에 띄우면 ~/.gradle/caches/modules-2.lock 경합이 나므로 순차)
#     3) 프론트(vite, 3000)를 포그라운드로 기동한다.
#        => 이 터미널에서 Ctrl+C 하면 프론트가 종료된다.
#
#   사전 준비(가이드 LOCAL_DEV.md 참고):
#     - Docker Desktop 이 떠 있어야 한다 (Redis/MySQL 컨테이너)
#     - CoreService/.env , AssistService/.env 가 있어야 한다 (DB 자격증명, DB_URL=...:3307)
#     - vite-react-teamsketch 에서 npm install 한 번 해두어야 한다
#
#   종료: 프론트 Ctrl+C 후 스크립트가 출력하는 taskkill 명령 사용
# =============================================================================

set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
cd "$ROOT"

LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

KILL_HINT="netstat -ano | grep LISTENING | grep -E ':(8080|8081|8082|3000)' | awk '{print \$NF}' | sort -u | xargs -r -n1 taskkill //F //PID"

echo "=== Haru 로컬 구동 시작 (루트: $ROOT) ==="

# ----------------------------------------------------------------------------
# 0) 사전 점검
# ----------------------------------------------------------------------------
for f in CoreService/.env AssistService/.env; do
  [ -f "$ROOT/$f" ] || echo "[!] 경고: $f 가 없습니다. '${f}.example' 를 복사해 DB_PASSWORD 를 채우세요." >&2
done
[ -d "$ROOT/vite-react-teamsketch/node_modules" ] || \
  echo "[!] 경고: node_modules 가 없습니다. 'cd vite-react-teamsketch && npm install' 하세요." >&2

# ----------------------------------------------------------------------------
# 1) 인프라 컨테이너 확인 / 기동
# ----------------------------------------------------------------------------
echo "[1/4] 인프라 컨테이너 확인..."
if ! docker info >/dev/null 2>&1; then
  echo "[!]   Docker 데몬에 연결할 수 없습니다. Docker Desktop 을 먼저 실행하세요." >&2
  exit 1
fi

DB_PASSWORD="$(grep '^DB_PASSWORD=' "$ROOT/CoreService/.env" | cut -d= -f2-)"

docker start haru-redis >/dev/null 2>&1 || \
  docker run -d --name haru-redis -p 6379:6379 --restart unless-stopped redis:7-alpine >/dev/null
docker start haru-mysql >/dev/null 2>&1 || \
  docker run -d --name haru-mysql -p 3307:3306 --restart unless-stopped \
    -e MYSQL_ROOT_PASSWORD="$DB_PASSWORD" mysql:8.4 --lower-case-table-names=0 >/dev/null

docker exec haru-redis redis-cli ping >/dev/null 2>&1 \
  && echo "      Redis  PONG" || echo "[!]   Redis 응답 없음" >&2

for i in $(seq 1 40); do
  docker exec haru-mysql mysqladmin ping -h127.0.0.1 --silent >/dev/null 2>&1 && break
  sleep 3
done
docker exec haru-mysql mysqladmin ping -h127.0.0.1 --silent >/dev/null 2>&1 \
  && echo "      MySQL  alive (3307)" || { echo "[!]   MySQL 응답 없음" >&2; exit 1; }

# 새로 만든 컨테이너에는 haru_db가 없다 → 마이그레이션 실행기로 init(기준선 + 전체 V*). 이미 있으면 migrate.
# 비밀번호는 명령줄에 남기지 않도록 환경변수로만 넘긴다.
if [ -z "$(docker exec -e MYSQL_PWD="$DB_PASSWORD" haru-mysql mysql -uroot -N -e "SHOW DATABASES LIKE 'haru_db'" 2>/dev/null)" ]; then
  echo "      haru_db 없음 → 스키마 초기화(init)"
  db_mode=init
else
  db_mode=migrate
fi
HARU_DB_PASSWORD="$DB_PASSWORD" "$ROOT/CoreService/gradlew.bat" -p "$ROOT/sql" run --console=plain -q \
  --args="$db_mode --url jdbc:mysql://127.0.0.1:3307/ --database haru_db --user root" \
  || { echo "[!]   스키마 $db_mode 실패 — sql/README.md 참고" >&2; exit 1; }

# ----------------------------------------------------------------------------
# 2) Spring 서비스 순차 기동
# ----------------------------------------------------------------------------
start_spring () {
  local name="$1" dir="$2" logfile="$3" pidfile="$4"

  echo "      → $name 기동 (로그: $logfile)"
  : > "$logfile"
  ( cd "$ROOT/$dir" && ./gradlew.bat bootRun --console=plain ) > "$logfile" 2>&1 &
  echo $! > "$pidfile"

  local waited=0
  until grep -qE "Started .*Application|Netty started|BUILD FAILED|APPLICATION FAILED" "$logfile" 2>/dev/null; do
    sleep 3; waited=$((waited + 3))
    if [ "$waited" -ge 180 ]; then
      echo "[!]   $name 180초 내 기동 확인 실패 → $logfile 확인" >&2
      echo "      이미 띄운 백엔드 종료: $KILL_HINT" >&2
      exit 1
    fi
  done
  # 실패 메시지도 대기를 끝내는 신호다. 성공으로 취급해 다음 서비스로 넘어가지 않는다.
  if grep -qE "BUILD FAILED|APPLICATION FAILED" "$logfile"; then
    echo "[!]   $name 기동 실패 → $logfile 확인" >&2
    echo "      이미 띄운 백엔드 종료: $KILL_HINT" >&2
    exit 1
  fi
}

echo "[2/4] GateWay 기동..."
start_spring "GateWay(8080)" "GateWay" "$LOG_DIR/gateway.log" "$LOG_DIR/gateway.pid"

echo "[3/4] CoreService / AssistService 기동..."
start_spring "CoreService(8081)" "CoreService" "$LOG_DIR/core.log" "$LOG_DIR/core.pid"
start_spring "AssistService(8082)" "AssistService" "$LOG_DIR/assist.log" "$LOG_DIR/assist.pid"

echo ""
echo "    백엔드 3개 기동 완료. 로그: tail -f logs/gateway.log logs/core.log logs/assist.log"
echo ""

# ----------------------------------------------------------------------------
# 3) 프론트(포그라운드) 기동
# ----------------------------------------------------------------------------
echo "[4/4] 프론트(vite, 3000) 기동... (종료하려면 Ctrl+C)"
cd "$ROOT/vite-react-teamsketch"
npm run dev

echo ""
echo "=== 프론트 종료됨. 백엔드는 아직 백그라운드에서 돌고 있습니다. ==="
echo "    백엔드 종료:  $KILL_HINT"
echo "    인프라 종료(원하면):  docker stop haru-redis haru-mysql"
