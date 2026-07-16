# 로컬 개발 환경 구동 가이드 (Haru)

마이크로서비스(GateWay / CoreService / AssistService + React 프론트)를 **본인 로컬 PC**에서 띄우는 절차다.
원격(sunbee.world / 도커)에서 로컬(localhost)로 전환한 상태이며, **FastAPI(AI/llama) 서비스는 이번 범위에서 제외**한다(맨 아래 "선택사항" 참고).

---

## 1. 포트 / 서비스 구성

| 서비스 | 종류 | 포트 | 역할 |
|---|---|---|---|
| GateWay | Spring Boot | **8080** | 진입점, JWT 인증, 라우팅 (프론트는 여기만 바라봄) |
| CoreService | Spring Boot | **8081** | 사용자/마켓/채팅. MySQL + Redis + WebSocket + OAuth2 |
| AssistService | Spring Boot | **8082** | 보조 기능(OCR/SMS/번역 등). MySQL |
| 프론트 | Vite + React | **3000** | 웹 UI |
| MySQL | DB | 3306 | `haru_db` |
| Redis | 캐시/세션 | 6379 | CoreService 전용 |
| ~~FastAPI~~ | (제외) | 8001 | AI/llama — 이번엔 안 띄움 |

요청 흐름: **프론트(3000) → GateWay(8080) → Core(8081) / Assist(8082)**
WebSocket: **프론트 → ws://localhost:8080/ws → GateWay → Core(8081)**

---

## 2. 사전 준비 (최초 1회)

### 2-1. 필수 도구 확인
```bash
java -version        # 21 설치돼 있으면 OK (Gradle toolchain이 17로 자동 처리)
mysql --version      # MySQL 클라이언트
redis-server --version
```

### 2-2. MySQL 준비
- 로컬 MySQL이 떠 있어야 한다 (`localhost:3306`).
- DB `haru_db` 와 테이블은 **이미 생성되어 있다고 가정**한다.
- (참고) 스키마 스크립트는 `MySQL/` 폴더에 `.txt` 형태로 흩어져 있다. 새로 적재할 때만 참고.
  - DB 생성: `MySQL/haru_db_생성.txt`
  - 테이블: `MySQL/User_table/`, `MySQL/market_table/`, `MySQL/board_table/`, `MySQL/chat/`, `MySQL/assistService_table/`
- 접속 확인:
  ```bash
  mysql -u root -p -e "USE haru_db; SHOW TABLES;"
  ```

### 2-3. Redis 준비
설치만 되어 있으면 된다. 구동은 `run-local.sh` 가 자동으로 해준다. 수동 확인:
```bash
redis-cli ping   # PONG 이 나오면 이미 떠 있음
```

### 2-4. 백엔드 .env 생성 (DB 자격증명) — **직접 생성 필요**
CoreService / AssistService 는 각 폴더의 `.env` 를 자동 로드한다(spring-dotenv).
`.env` 는 비밀번호가 들어가므로 git 에 커밋하지 않으며, **본인이 직접 만들어야 한다.**

```bash
# CoreService
cp CoreService/.env.example CoreService/.env
# 편집기로 열어 DB_PASSWORD 에 실제 로컬 MySQL 비밀번호 입력
#   DB_URL / DB_USERNAME 도 본인 환경에 맞게 확인

# AssistService
cp AssistService/.env.example AssistService/.env
# DB_PASSWORD 입력. 외부 API 키(SMS/OCR/KAKAO 등)는 로컬에서 안 쓰면 빈 값이어도 기동됨
```
> 이미 `.env` 를 만들어 둔 상태라면 그대로 두면 된다. `.env.example` 은 참고용 템플릿일 뿐이다.

### 2-5. 프론트 환경변수 (대개 그대로 OK)
프론트는 `vite-react-teamsketch/.env.development` 를 dev 모드에서 자동 로드한다(이미 localhost:8080 을 가리킴).
게이트웨이 주소를 바꾸고 싶을 때만 `.env.local` 로 덮어쓴다:
```bash
cd vite-react-teamsketch
cp .env.example .env.local   # 필요할 때만
```
필요한 키:
- `VITE_API_URL` = `http://localhost:8080/api` (게이트웨이 REST, **/api 까지 포함**)
- `VITE_WS_URL`  = `ws://localhost:8080/ws` (게이트웨이 WebSocket)

### 2-6. 프론트 의존성 설치 (최초 1회)
```bash
cd vite-react-teamsketch
npm install
```

---

## 3. 구동

### 3-1. 한 번에 (권장)
루트에서:
```bash
./run-local.sh
```
- Redis 자동 기동 확인/시작
- GateWay → Core → Assist 순으로 백그라운드 기동 (로그: `logs/*.log`)
- 프론트 dev 서버를 **포그라운드**로 기동 (Ctrl+C 로 종료)

종료: 프론트는 Ctrl+C, 백엔드는 스크립트 하단 주석 참고(또는 `logs/*.pid` 사용).

### 3-2. 수동 (서비스별로 따로 띄우고 싶을 때)
각각 별도 터미널에서:
```bash
# 1) MySQL 가 떠 있는지 확인 (2-2)
# 2) Redis
redis-server --daemonize yes

# 3) GateWay
cd GateWay && ./gradlew bootRun

# 4) CoreService
cd CoreService && ./gradlew bootRun

# 5) AssistService
cd AssistService && ./gradlew bootRun

# 6) 프론트
cd vite-react-teamsketch && npm run dev
```
> Spring 서비스는 기동에 수십 초 걸린다. Core 는 MySQL + Redis 가 먼저 떠 있어야 정상 기동한다.

기동 순서 권장: **MySQL → Redis → GateWay/Core/Assist(순서 무관하나 Core는 DB/Redis 선행) → 프론트**

---

## 4. 헬스체크

```bash
# 포트가 LISTEN 중인지
lsof -i :8080 -i :8081 -i :8082 -i :3000 -i :6379 -i :3306 | grep LISTEN

# Redis
redis-cli ping                       # PONG

# 게이트웨이 살아있는지 (응답 코드만 확인)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/

# 프론트
open http://localhost:3000
```
- 게이트웨이 콘솔 로그에 `현재 활성화된 프로필: dev`, `Core URI: http://localhost:8081` 가 보이면 로컬 라우팅 정상.
- 브라우저 콘솔에 `BACKEND_URL ws://localhost:8080/ws` 가 찍히면 프론트 WS 설정 정상.

---

## 5. 트러블슈팅

### 포트 충돌 (Address already in use)
```bash
lsof -i :8080     # 점유 프로세스 PID 확인
kill -9 <PID>
```
8081/8082/3000/6379 도 동일.

### Redis 미기동 (Core 기동 실패 / Unable to connect to Redis)
```bash
redis-cli ping          # 응답 없으면
redis-server --daemonize yes
```

### DB 접속 실패 (Access denied / Unknown database / Communications link failure)
- MySQL 데몬이 떠 있는지: `mysqladmin -u root -p ping`
- `haru_db` 존재 여부: `mysql -u root -p -e "SHOW DATABASES;"`
- `CoreService/.env` · `AssistService/.env` 의 `DB_USERNAME` / `DB_PASSWORD` / `DB_URL` 이 실제 로컬 계정과 일치하는지 확인.
- 비밀번호에 `@`, `#`, `&` 같은 특수문자가 있어도 `.env` 에는 따옴표 없이 그대로 적는다.

### 프론트가 API를 못 부름 (CORS / Network Error)
- GateWay(8080) 가 떠 있는지 확인. 프론트는 **항상 게이트웨이만** 호출한다.
- `.env.development` / `.env.local` 의 `VITE_API_URL` 이 `http://localhost:8080/api` 인지 확인.
- env 를 바꿨으면 **vite dev 서버를 재시작**해야 반영된다.

### gradlew Permission denied
```bash
chmod +x GateWay/gradlew CoreService/gradlew AssistService/gradlew
```

---

## 6. 선택사항 / 추후

- **FastAPI(AI/llama)**: 이번 로컬 구동 범위에서 제외. 추후 띄우려면 `FastAPI/` 참고(8001 포트). 안 띄워도 핵심 기능(인증/마켓/채팅)은 동작한다.
- **도커/원격 배포**: `GATEWAY_PROFILE=prod` 로 기동하면 컨테이너 이름(`core-container` 등) 기반 라우팅으로 전환된다. 로컬에서는 설정하지 않는다(기본 dev).
