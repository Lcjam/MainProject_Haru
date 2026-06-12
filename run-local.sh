#!/usr/bin/env bash
# =============================================================================
# Haru 로컬 개발 구동 스크립트 (macOS / zsh·bash)
#
#   사용법:  ./run-local.sh
#
#   동작:
#     1) Redis 가 떠 있는지 확인하고, 없으면 띄운다
#     2) GateWay(8080) → CoreService(8081) → AssistService(8082) 3개 Spring
#        서비스를 백그라운드로 기동한다. 로그는 logs/<서비스>.log 에 쌓인다.
#     3) 프론트(vite, 3000)를 포그라운드로 기동한다.
#        => 이 터미널에서 Ctrl+C 하면 프론트가 종료된다.
#
#   사전 준비(가이드 LOCAL_DEV.md 참고):
#     - 로컬 MySQL(localhost:3306, haru_db) 가 떠 있어야 한다 (이 스크립트는 띄우지 않음)
#     - CoreService/.env , AssistService/.env 가 있어야 한다 (DB 자격증명)
#     - vite-react-teamsketch 에서 npm install 한 번 해두어야 한다
#
#   백엔드 종료 방법 (프론트 Ctrl+C 한 뒤):
#     ./run-local.sh 가 logs/<서비스>.pid 에 PID를 남긴다. 아래로 종료:
#         kill $(cat logs/gateway.pid logs/core.pid logs/assist.pid) 2>/dev/null
#     혹은 포트로 강제 종료:
#         lsof -ti :8080 -ti :8081 -ti :8082 | xargs kill -9 2>/dev/null
#     Redis 종료(원하면):  redis-cli shutdown
# =============================================================================

set -u

# 스크립트가 있는 디렉토리를 프로젝트 루트로 사용
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
cd "$ROOT"

LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

echo "=== Haru 로컬 구동 시작 (루트: $ROOT) ==="

# ----------------------------------------------------------------------------
# 0) 사전 점검
# ----------------------------------------------------------------------------
for svc in GateWay CoreService AssistService; do
  if [ ! -x "$ROOT/$svc/gradlew" ]; then
    echo "[*] $svc/gradlew 에 실행권한 부여"
    chmod +x "$ROOT/$svc/gradlew"
  fi
done

if [ ! -f "$ROOT/CoreService/.env" ]; then
  echo "[!] 경고: CoreService/.env 가 없습니다. 'cp CoreService/.env.example CoreService/.env' 후 DB_PASSWORD 를 채우세요."
fi
if [ ! -f "$ROOT/AssistService/.env" ]; then
  echo "[!] 경고: AssistService/.env 가 없습니다. 'cp AssistService/.env.example AssistService/.env' 후 DB_PASSWORD 를 채우세요."
fi

# ----------------------------------------------------------------------------
# 1) Redis 확인 / 기동
# ----------------------------------------------------------------------------
echo "[1/4] Redis 확인..."
if redis-cli ping >/dev/null 2>&1; then
  echo "      Redis 이미 기동됨 (PONG)"
else
  echo "      Redis 미기동 → 백그라운드로 시작"
  redis-server --daemonize yes
  sleep 1
  if redis-cli ping >/dev/null 2>&1; then
    echo "      Redis 기동 완료"
  else
    echo "[!]   Redis 기동 실패. redis-server 설치 여부를 확인하세요." >&2
  fi
fi

# ----------------------------------------------------------------------------
# 2) Spring 서비스 백그라운드 기동
# ----------------------------------------------------------------------------
start_spring () {
  local name="$1"      # 표시용 이름
  local dir="$2"       # 서비스 폴더
  local logfile="$3"   # 로그 파일
  local pidfile="$4"   # pid 파일

  echo "      → $name 기동 (로그: $logfile)"
  ( cd "$ROOT/$dir" && ./gradlew bootRun ) > "$logfile" 2>&1 &
  echo $! > "$pidfile"
}

echo "[2/4] GateWay 기동..."
start_spring "GateWay(8080)" "GateWay" "$LOG_DIR/gateway.log" "$LOG_DIR/gateway.pid"

echo "[3/4] CoreService / AssistService 기동..."
# Core 는 MySQL + Redis 선행 필요 (위에서 Redis 확인 완료, MySQL 은 사용자 책임)
start_spring "CoreService(8081)" "CoreService" "$LOG_DIR/core.log" "$LOG_DIR/core.pid"
start_spring "AssistService(8082)" "AssistService" "$LOG_DIR/assist.log" "$LOG_DIR/assist.pid"

echo ""
echo "    백엔드 3개를 백그라운드로 기동했습니다. 기동에 수십 초 걸립니다."
echo "    진행 로그 보기:  tail -f logs/gateway.log logs/core.log logs/assist.log"
echo ""

# ----------------------------------------------------------------------------
# 3) 프론트(포그라운드) 기동
# ----------------------------------------------------------------------------
echo "[4/4] 프론트(vite, 3000) 기동... (종료하려면 Ctrl+C)"
if [ ! -d "$ROOT/vite-react-teamsketch/node_modules" ]; then
  echo "[!]   node_modules 가 없습니다. 먼저 'cd vite-react-teamsketch && npm install' 하세요." >&2
fi

cd "$ROOT/vite-react-teamsketch"
npm run dev

# 프론트가 Ctrl+C 로 종료된 뒤 안내
echo ""
echo "=== 프론트 종료됨. 백엔드는 아직 백그라운드에서 돌고 있습니다. ==="
echo "    백엔드 종료:  kill \$(cat logs/gateway.pid logs/core.pid logs/assist.pid) 2>/dev/null"
echo "    포트로 강제 종료:  lsof -ti :8080 -ti :8081 -ti :8082 | xargs kill -9 2>/dev/null"
echo "    Redis 종료(원하면):  redis-cli shutdown"
