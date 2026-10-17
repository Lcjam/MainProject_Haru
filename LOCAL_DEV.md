# 로컬 개발 환경 구동 가이드 (Haru)

이 문서는 **WSL2 Ubuntu** 기준이다. 작업 루트는 **`~/code/haru`**이며, `/mnt/c`에서 작업하지 않는다(9p 경유라 Gradle/Vite가 느려지고 파일 감시가 깨진다). Windows 쪽 원본 체크아웃이 있다면 롤백용 보존본으로만 두고 개발 대상으로 쓰지 않는다.

로컬 구성은 프론트(3000) → GateWay(8080) → CoreService(8081) / AssistService(8082)이며, CoreService는 MySQL과 Redis를 사용하고 AssistService는 MySQL을 사용한다. FastAPI(8001)는 이 가이드의 필수 범위가 아니다.

**기본 기동 경로는 compose다.** 컨테이너 안에서 빌드하므로 로컬 JDK/Node 없이도 전 스택이 뜬다. 아래 "실행 — compose" 참조. 로컬 툴체인은 개별 서비스 디버깅(`bootRun`)과 프론트 HMR(`npm run dev`)에만 필요하다.

## 사전 준비

- **Docker Desktop**(WSL2 백엔드)에서 Ubuntu 통합을 켠다. Windows와 WSL이 같은 데몬을 공유한다. **부팅 직후에는 통합이 아직 붙지 않아 `docker: command not found`가 난다** — `docker ps`가 될 때까지 기다린 뒤 진행한다.
- JDK **17**: `sudo apt install openjdk-17-jdk`. 기본 `java`는 21이어도 되며, `/usr/lib/jvm/java-17-openjdk-amd64`가 있으면 Gradle toolchain이 자동 탐지하므로 `gradle.properties` 설정이 필요 없다. Gradle이 JDK를 자동 다운로드한다고 가정하지 않는다.
- **Node.js/npm은 Linux 것이어야 한다**: `sudo apt install nodejs npm` 후 `command -v npm`이 `/usr/bin/npm`인지 확인한다. `/mnt/c/Program Files/nodejs/npm`이면 Windows npm이 PATH를 잡은 것이고, 그대로 `npm ci` 하면 Windows용 esbuild/rollup 바이너리가 설치돼 Vite 빌드가 깨진다.
- 프론트 의존성은 lockfile을 재현하도록 최초 1회 `npm ci`를 실행한다.
- (선택) `sudo apt install mysql-client redis-tools` — DB 점검용.

| 서비스 | 포트 | 용도 |
|---|---:|---|
| GateWay | 8080 | JWT 인증·라우팅 |
| CoreService | 8081 | 사용자·마켓·채팅·WebSocket |
| AssistService | 8082 | OCR·SMS·번역 등 보조 기능 |
| 프론트 | 3000 | Vite 개발 서버 |
| MySQL — compose `mysql` | (비공개) | `haru_db`. `expose`만 되어 호스트에서 직접 접근 불가 |
| MySQL — 컨테이너 `haru-mysql` | **3307** | 네이티브 기동·통합 테스트용. compose와 데이터 별개 |
| Redis — compose `redis` | (비공개) | `requirepass` 사용 |
| Redis — 컨테이너 `haru-redis` | 6379 | 무인증. 네이티브 기동·통합 테스트용 |

compose 스택에서 호스트로 published 되는 포트는 **gateway(8080)와 frontend(3000)뿐**이다. WSL2 포트 포워딩으로 Windows 브라우저에서도 `localhost:3000`·`8080`이 그대로 열린다.

## 데이터베이스와 Redis

MySQL은 **`lower_case_table_names=0`인 Linux MySQL 8.4가 필수**다. 마이그레이션 실행기가 그 외 설정을 거부한다(`Migration refused: R02 requires a MySQL server with lower_case_table_names=0`). Windows 네이티브 MySQL은 이 값이 `1`이라 쓸 수 없으므로 컨테이너를 쓰고, 네이티브 MySQL이 3306을 잡고 있으므로 호스트 포트는 **3307**을 쓴다.

```bash
PW=$(grep '^DB_PASSWORD=' CoreService/.env | cut -d= -f2-)

docker start haru-redis 2>/dev/null || \
  docker run -d --name haru-redis -p 6379:6379 --restart unless-stopped redis:7-alpine
docker start haru-mysql 2>/dev/null || \
  docker run -d --name haru-mysql -p 3307:3306 --restart unless-stopped \
    -e MYSQL_ROOT_PASSWORD="$PW" mysql:8.4 --lower-case-table-names=0

docker exec haru-redis redis-cli ping                          # PONG
docker exec haru-mysql mysqladmin ping -h127.0.0.1 --silent    # alive
```

DB 작업은 [`sql/README.md`](sql/README.md)의 실행 명령을 따른다. `init`은 빈 DB에 baseline + V1을 적용한다. 기존 DB에 실행기 이력이 없으면 확인된 스키마 버전에 맞춰 `adopt --version 0`(baseline) 또는 `adopt --version 1`(V1까지 적용)을 실행한 뒤 `migrate`한다. 이미 이력이 있는 DB는 `migrate`만 실행한다. 비밀번호는 `HARU_DB_PASSWORD` 환경변수로 주입하며, 실행기는 서비스의 `.env`를 자동 로드하지 않는다.

```bash
export HARU_DB_PASSWORD=$(grep '^DB_PASSWORD=' CoreService/.env | cut -d= -f2-)
./CoreService/gradlew -p sql run --no-daemon --console=plain \
  --args="init --url jdbc:mysql://127.0.0.1:3307/ --database haru_db --user root"
docker exec -i haru-mysql mysql -uroot -p"$HARU_DB_PASSWORD" < sql/seed-local.sql   # 선택적 seed
unset HARU_DB_PASSWORD
```

`schema.sql`은 DROP 구문이 있으므로 기존 DB에 직접 재실행하지 않는다. 현재 도메인 테이블은 baseline 20개 + V1 7개이며, 실행기 이력 테이블(`haru_schema_history`)은 별도다. 이후 변경은 `sql/migrations/V*.sql`로 누적한다. compose로 기동하면 `migrator` 서비스가 이 `init`을 자동으로 수행하므로 아래 수동 절차는 네이티브 기동(`haru-mysql`, 3307) 전용이다.

## 환경변수

CoreService와 AssistService는 `me.paulschwarz:spring-dotenv`를 사용해 각 서비스 폴더의 `.env`를 로드한다. 아래 복사는 `.env`가 없을 때만 실행하며 기존 파일은 보존한다. 복사 후 DB 자격증명을 입력하고 `.env`는 커밋하지 않는다.

```bash
cp CoreService/.env.example CoreService/.env
cp AssistService/.env.example AssistService/.env
```

필수 DB 키는 두 파일 모두 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`이며 URL은 **`jdbc:mysql://127.0.0.1:3307/haru_db`** 이다(컨테이너 포트). GateWay에는 `spring-dotenv`가 없으므로 GateWay의 `JWT_SECRET`/`JWT_EXPIRATION`은 OS 환경변수 또는 실행 환경에서 주입한다. 세 Spring 서비스에 `JWT_SECRET`을 지정한다면 같은 값을 사용한다. 미지정 시 코드에 로컬 개발용 기본값이 있지만 운영에서 사용하지 않는다. `JWT_SECRET=`처럼 빈 값을 명시해 기본값을 덮어쓰지 않는다.

환경변수 이름은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `OAUTH2_REDIRECT_URI`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`이다. AssistService 외부 연동 키는 `SMS_API_KEY`, `SMS_API_SECRET`, `SMS_API_URL`, `NAVER_CLOUD_CHATBOT_SECRET_KEY`, `NAVER_CLOUD_CHATBOT_API_URL`, `NAVER_CLOUD_OCR_SECRET_KEY`, `NAVER_CLOUD_OCR_API_URL`, `KAKAO_REST_API_KEY`, `TRANSLATE_API_KEY`, `TRANSLATE_API_EMAIL`, `GATEWAY_BASE_URL`이다. Assist 예제의 외부 연동 키 줄은 properties의 필수 placeholder 해석을 위해 원래처럼 빈 값으로 남겨 둔다. 실제 연동을 사용하지 않는 값은 빈 값 정책을 코드와 확인한 뒤 변경한다.

Redis는 현재 CoreService 설정의 `localhost:6379`를 사용한다. OS 환경변수 `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_DATA_REDIS_PASSWORD`로 Spring 설정을 덮어쓸 수 있다. `GATEWAY_PROFILE`은 GateWay의 라우팅 선택값이다. 기본 `dev`에서는 Core/Assist/FastAPI 주소가 localhost이고 `prod`에서는 컨테이너 이름을 사용한다. URI 자체는 `RouteConfig`에 고정돼 있다. `GATEWAY_BASE_URL`은 Assist가 FastAPI를 호출할 때 거치는 Gateway 주소다.

프론트는 Vite가 `vite-react-teamsketch/.env.development`를 자동 로드한다. 게이트웨이 주소를 덮어쓸 때만 `.env.local`을 만든다. 키는 `VITE_API_URL=http://localhost:8080/api`, `VITE_WS_URL=ws://localhost:8080/ws`이며 변경 후 Vite를 재시작한다.

## 실행

### compose (기본)

최초 1회 루트 `.env`를 만든다. 루트 `.env`는 Git이 무시하므로 실제 값을 넣어도 커밋되지 않는다.

```bash
cd ~/code/haru
cp .env.compose.example .env     # MYSQL_ROOT_PASSWORD / REDIS_PASSWORD / JWT_SECRET 채움
chmod 600 .env
```

```bash
docker compose up -d                  # 백엔드만 (mysql redis migrator core assist gateway)
docker compose --profile ui up -d     # 프론트(nginx, 3000)까지
docker compose ps                     # 전부 healthy 인지 확인
```

프로젝트명은 `.env`의 `COMPOSE_PROJECT_NAME`(기본 `haru-dev`)이 정하며, 볼륨 이름도 여기서 파생된다. **프로젝트명을 바꾸면 다른 볼륨을 쓰게 되어 빈 DB가 만들어진다.** `migrator` 서비스가 기동 시 `init`을 수행하고 `Initialized and migrated database haru_db to version 1`을 남긴 뒤 정상 종료(Exited 0)한다.

종료는 다음과 같다. `-v`는 데이터 볼륨까지 지우므로 DB를 새로 구성할 때만 쓴다.

```bash
docker compose --profile ui down
docker compose --profile ui down -v
```

FastAPI는 `--profile ai`로 별도 기동한다.

### 네이티브 실행 (개별 서비스 디버깅·프론트 HMR)

인프라 컨테이너를 먼저 띄운다. `haru-mysql`/`haru-redis`는 compose와 별개이며 데이터도 별개다. 각 명령은 저장소 루트에서 시작하는 별도 터미널에서 실행한다. 래퍼는 `./gradlew`(sh)이며 `.bat`은 Windows 보존본 전용이다.

```bash
cd vite-react-teamsketch && npm ci
```
```bash
cd GateWay && ./gradlew bootRun
```
```bash
cd CoreService && ./gradlew bootRun
```
```bash
cd AssistService && ./gradlew bootRun
```
```bash
cd vite-react-teamsketch && npm run dev
```

권장 순서는 MySQL → Redis → Spring 서비스 → 프론트다. 동시 기동은 `~/.gradle/caches/modules-2.lock` 경합을 일으키므로 하나씩 `Started Application` / `Netty started`를 확인한 뒤 다음을 띄운다.

## 점검

반복 API 점검은 [Postman 실행 안내](api-tests/README.md)를 따른다. 기동 후 저장소 루트에서 아래 명령을 실행하면 공개 API 응답과 전체 REST 경로의 잘못된 토큰 거부를 확인한다. 로그인한 사용자의 조회와 게시판 CRUD는 안내에 따라 별도로 실행한다.

```bash
npx --yes newman@6.2.3 run api-tests/haru.postman_collection.json \
  --folder "01 Startup" --folder "03 Route auth" --timeout-request 5000
```

```bash
# 게이트웨이→Core→DB 왕복 (정상 = 400 JSON)
curl -4 -s -X POST -H "Content-Type: application/json" -d '{}' http://127.0.0.1:8080/api/core/auth/login
curl -4 -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/core/   # 401 (JWT 필터)
curl -4 -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:3000/            # 200
curl -4 -s -o /dev/null -w "%{http_code}\n" -X POST -H "Content-Type: application/json" -d '{}' http://127.0.0.1:3000/api/core/auth/login  # 400 (nginx→gateway→Core, ui 프로필)
ss -ltnp | grep -E ":(8080|8081|8082|3000)\b"                                # 포트 확인
docker compose ps                                                            # compose 스택 상태
docker exec haru-redis redis-cli ping                                        # PONG (네이티브 기동 시)
```

WebSocket 연결 대상은 `ws://localhost:8080/ws`다. 주소 목록은 연결 대상이며 `/api` 자체의 200 응답을 health 검사로 사용하지 않는다. 네이티브 실행은 해당 터미널에서 Ctrl+C로, 백그라운드로 띄웠다면 아래로 종료한다.

```bash
pkill -f 'GradleWrapperMain|bootRun'
pkill -f vite
docker stop haru-redis haru-mysql   # 인프라까지 내릴 때만 (rm 하지 말 것)
```

포트가 이미 사용 중이면 위 명령으로 종료한 뒤 재시작한다. **Windows 보존본과 WSL 스택을 동시에 띄우면 8080/3000이 충돌하므로 한 번에 한쪽만 기동한다.** DB 연결 오류는 두 백엔드 `.env`의 `DB_*` 값(특히 포트 3307)과 `docker ps`의 `haru-mysql` 상태를 확인한다. compose 쪽 오류는 `docker compose logs <service> --tail 50`으로 본다. `MySQL/`의 구세대 `.txt` 스크립트는 현재 스키마 기준선이 아니므로 사용하지 않는다.

## 선택사항

FastAPI(AI/llama)는 기본 로컬 실행 범위에서 제외한다. 필요할 때만 `FastAPI/`와 해당 서비스 문서를 확인한다.
