# 개발 가이드

로컬에서 Haru를 실행하고 테스트하는 방법입니다. 방법은 두 가지입니다.

| 방법 | 필요한 것 | 특징 |
|---|---|---|
| **A. Docker Compose** (권장) | Docker | MySQL·Redis·마이그레이션·백엔드를 한 번에 띄움 |
| **B. 로컬 직접 실행** | JDK 17, Node 22, MySQL 8, Redis | 서비스별로 코드를 고치며 실행할 때 |

## 포트

| 서비스 | 포트 | 비고 |
|---|---|---|
| GateWay | 8080 | 프론트엔드가 호출하는 유일한 주소 |
| CoreService | 8081 | |
| AssistService | 8082 | |
| FastAPI | 8001 | 선택 |
| Frontend | 3000 | |
| MySQL | 3306 | Compose에서는 외부에 노출하지 않음 |
| Redis | 6379 | Compose에서는 외부에 노출하지 않음 |

---

## A. Docker Compose

```bash
cp .env.compose.example .env
# .env 의 MYSQL_ROOT_PASSWORD, REDIS_PASSWORD, JWT_SECRET 를 임의의 값으로 바꾼다

docker compose up -d --build                         # MySQL, Redis, migrator, Core, Assist, GateWay
docker compose --profile ui up -d --build            # + 프론트엔드 (http://localhost:3000)
docker compose --profile ai up -d --build            # + FastAPI 챗봇 (모델 파일 필요)
```

- 기동 순서는 다음과 같습니다.
  1. MySQL·Redis가 healthy 상태가 됩니다.
  2. `migrator`가 스키마를 적용합니다.
  3. Core·Assist가 뜹니다.
  4. GateWay가 뜹니다.
- 게이트웨이와 프론트엔드는 `127.0.0.1`에만 바인딩됩니다.
- 처음 실행했다면 참조 데이터(카테고리·취미)와 테스트 계정을 넣으세요. [DB 문서의 로컬 시드](database.md#로컬-시드-데이터)를 참고하세요.

```bash
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < sql/seed-local.sql
```

- **FastAPI 프로필(`ai`)**: `LLAMA_MODEL_DIR`에 gguf 모델 파일을 두어야 합니다. 기본 파일명은 `tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf`입니다. 모델 파일은 용량이 커서 저장소에 포함하지 않습니다.
- **외부 연동 키**(SMS, 네이버 클라우드 OCR/챗봇, 카카오)는 비워 두어도 기동은 됩니다. 해당 기능만 동작하지 않습니다.

```bash
docker compose logs -f core   # 로그 확인
docker compose down           # 종료 (데이터 유지)
docker compose down -v        # 종료 + 볼륨 삭제
```

---

## B. 로컬 직접 실행

### 1. 사전 준비

- **JDK 17**: Gradle toolchain이 17을 요구합니다.
- **Node 22**, MySQL 8, Redis
- `haru_db` 스키마를 준비합니다. [마이그레이션 러너](database.md#마이그레이션-러너)로 `init`한 뒤 `sql/seed-local.sql`을 적용하세요.

### 2. 환경변수 파일

CoreService와 AssistService는 [spring-dotenv](https://github.com/paulschwarz/spring-dotenv)로 각 폴더의 `.env`를 읽습니다.

```bash
cp CoreService/.env.example CoreService/.env       # DB_PASSWORD 입력
cp AssistService/.env.example AssistService/.env   # DB_PASSWORD 입력
```

- `JWT_SECRET`을 설정한다면 **GateWay·CoreService·AssistService에 같은 값**을 넣어야 합니다.
  - 설정하지 않으면 세 서비스가 같은 로컬 개발용 기본값을 씁니다.
- `.env`에 **빈 값**(`JWT_SECRET=`)을 두지 마세요. 빈 문자열이 기본값을 덮어써서 기동에 실패합니다.
  - 쓰지 않을 항목은 주석 상태로 두세요.
- 프론트엔드는 `vite-react-teamsketch/.env.development`(게이트웨이 `localhost:8080`)를 자동으로 읽습니다.
  - 주소를 바꿀 때만 `.env.local`로 덮어쓰세요.

### 3. 실행

**macOS 스크립트**

```bash
./run-local.sh
```

- Redis 실행 여부를 확인하고, 꺼져 있으면 켭니다.
- GateWay·Core·Assist를 백그라운드로 띄웁니다. 로그는 `logs/*.log`에 쌓입니다.
- 프론트엔드는 포그라운드로 띄웁니다. MySQL은 직접 켜 두어야 합니다.
- 백엔드 종료: `kill $(cat logs/gateway.pid logs/core.pid logs/assist.pid)`

**서비스별 수동 실행** (각각 별도 터미널에서)

```bash
cd GateWay && ./gradlew bootRun
cd CoreService && ./gradlew bootRun      # MySQL·Redis가 먼저 떠 있어야 함
cd AssistService && ./gradlew bootRun
cd vite-react-teamsketch && npm install && npm run dev
```

---

## 테스트

### 단위·계약 테스트 (DB 불필요)

```bash
cd CoreService && ./gradlew test      # @Tag("mysql"), @Tag("redis") 제외
cd AssistService && ./gradlew test
cd GateWay && ./gradlew test
cd vite-react-teamsketch && npm test && npm run build
```

### 통합 테스트 (실제 MySQL / Redis)

통합 테스트는 지정한 서버에 임시 DB와 임시 키를 만들고 끝나면 지웁니다. 그래도 **테스트 전용 서버**를 쓰세요.

```bash
export HARU_TEST_MYSQL_URL='jdbc:mysql://localhost:3307/?allowPublicKeyRetrieval=true&useSSL=false'  # DB 이름 없이 서버까지만
export HARU_TEST_MYSQL_USER=root
export HARU_TEST_MYSQL_PASSWORD='<비밀번호>'   # 빈 값도 허용되지만 변수 자체는 있어야 함
export HARU_TEST_REDIS_HOST=localhost
export HARU_TEST_REDIS_PORT=6380
export HARU_TEST_REDIS_PASSWORD='<비밀번호>'   # 선택

cd CoreService
./gradlew mysqlIntegrationTest        # 승인·거래·결제·위치·매퍼 통합 테스트
./gradlew redisIntegrationTest
./gradlew jacocoIntegrationReport     # 단위 + MySQL + Redis 레인 통합 커버리지 리포트
```

마이그레이션 러너 자체의 테스트는 `sql/`에서 실행합니다. 시스템 Gradle이 필요합니다.

```bash
gradle -p sql test                    # 단위
gradle -p sql mysqlIntegrationTest    # 전용 MySQL 필요 (위와 같은 HARU_TEST_MYSQL_* 사용)
```

### CI

`.github/workflows/ci.yml`이 PR과 `main` push마다 다음을 실행합니다.

- CoreService · AssistService · GateWay: `./gradlew test`
- Frontend: `npm ci`, `npm test`, `npm run build`

MySQL·Redis 통합 테스트 레인은 아직 CI에 포함되어 있지 않습니다. 로컬에서 실행하세요.

> `.github/workflows-disabled/`에는 예전 배포 워크플로가 보관되어 있습니다. 현재는 실행되지 않습니다.

---

## 트러블슈팅

| 증상 | 확인할 것 |
|---|---|
| `Address already in use` | `lsof -i :8080` 등으로 점유 프로세스를 확인하고 종료 |
| Core 기동 실패: `Unable to connect to Redis` | `redis-cli ping`. 응답이 없으면 `redis-server --daemonize yes` |
| `Access denied` / `Unknown database` | 각 서비스 `.env`의 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`, `haru_db` 존재 여부 |
| 모든 요청이 401 | 세 서비스의 `JWT_SECRET`이 같은지 확인. 하나만 다르면 해당 서비스가 모든 토큰을 거부함 |
| 회원가입 화면의 카테고리·취미가 비어 있음 | `sql/seed-local.sql` 적용 여부 |
| 프론트엔드에서 CORS / Network Error | GateWay(8080) 실행 여부, `VITE_API_URL`이 `http://localhost:8080/api`인지. env를 바꿨다면 Vite 재시작 |
| `gradlew: Permission denied` | `chmod +x GateWay/gradlew CoreService/gradlew AssistService/gradlew` |
| `Cannot find a Java installation ... languageVersion=17` | JDK 17 설치 |
