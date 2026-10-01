# 데이터베이스

- **DBMS**: MySQL 8
- **스키마**: `haru_db` (문자셋 `utf8mb4`)
- **접근 방식**: MyBatis (XML 매퍼 + 일부 어노테이션 매퍼)

## 스키마 기준

스키마의 기준은 `sql/` 디렉터리입니다.

| 파일 | 내용 |
|---|---|
| `sql/schema.sql` | 기준선(baseline) 덤프. 테이블 20개 |
| `sql/migrations/V1__board_location_tables.sql` | 매퍼가 참조하지만 DDL이 없던 테이블 7개와 `Users.profile_image_path` 컬럼 복원 |
| `sql/migrations/V2__user_location_latest.sql` | 사용자별 최신 위치 테이블 `UserLocationLatest` 추가 |
| `sql/seed-local.sql` | 로컬 개발용 시드 (테스트 계정 2개, 카테고리·취미 참조 데이터) |
| `sql/verify/mapper-smoke.sql` | 과거 수동 매퍼 스모크 SQL. **데이터를 넣으므로 스크래치 DB에서만** 실행 |

모든 마이그레이션을 적용하면 테이블은 28개이고, 이력 테이블 `haru_schema_history`가 따로 하나 더 생깁니다.

> **변경 규칙**: `schema.sql`은 다시 덤프하지 않습니다. 스키마를 바꿀 때는 `sql/migrations/`에 다음 번호의 `V{n}__설명.sql` 파일을 추가합니다.

## 도메인별 테이블

| 도메인 | 테이블 |
|---|---|
| 회원 | `Users`, `User_Account_info`, `PointsDopamineActivity` |
| 취미 | `categories`, `hobbies`, `category_hobbies`, `user_hobbies` |
| 마켓 | `Products`, `ProductImages`, `productrequests`, `Transactions`, `Payments` |
| 위치 | `UserLocation`, `UserLocationLatest`(사용자 최신 위치), `locations`(채팅방 실시간 위치) |
| 게시판 | `boards`, `board_members`, `board_posts`, `board_post_images`, `board_comments`, `board_post_reactions` |
| 채팅 | `chatrooms`, `messages` |
| AI 챗봇 (Assist) | `chat_messages` |
| 미사용 (레거시) | `Posts`, `Comments`, `Notices`, `UnreadNotices` — 현재 코드에서 참조하지 않음 |

주요 관계는 다음과 같습니다.

- `Users` ↔ `hobbies`: 다대다 (`user_hobbies`)
- `Users` → `Products`: 일대다
- `Products` → `productrequests` → `Transactions` → `Payments`: 상품 신청 → 승인 → 거래 → 결제
- `Products` → `chatrooms` → `messages`: 상품별 채팅방과 메시지
- `boards` → `board_posts` → `board_comments` / `board_post_reactions`

> 테이블 이름의 대소문자 규칙이 섞여 있습니다(`Users` / `boards`). 영향 범위가 커서 물리 이름은 바꾸지 않았습니다.
> Docker Compose의 MySQL은 `--lower-case-table-names=0`(대소문자 구분)으로 실행됩니다. 매퍼의 테이블 이름도 스키마와 대소문자까지 맞춰야 합니다.

## 마이그레이션 러너

`sql/src/main/java/com/haru/migration/`에 있는 JDBC 기반 러너가 스키마를 적용하고 검증합니다.

| 명령 | 용도 |
|---|---|
| `init` | **빈 DB 전용.** 기준선과 모든 마이그레이션을 적용하고 이력을 기록합니다. 대상 DB에 객체가 하나라도 있으면 거부합니다. |
| `adopt --version 0\|1` | 이력 없이 만들어진 기존 DB를 러너 관리로 편입합니다. 실제 스키마가 지정한 버전과 일치하는지 먼저 검사합니다. |
| `migrate` | 기록된 이력을 기준으로 남은 마이그레이션을 적용합니다. |

공통 옵션은 다음과 같습니다.

```
--url jdbc:mysql://host:port/   # 서버까지만 지정 (DB 경로·자격증명 포함 금지)
--database haru_db
--user root
--lock-timeout 30               # 선택, 0~300초
```

- 비밀번호는 `HARU_DB_PASSWORD` 환경변수로만 받습니다.
- 동시 실행은 MySQL named lock으로 직렬화합니다.
- 각 단계 전후로 실제 스키마가 기대한 스키마와 같은지 검사합니다. 실패하면 `FAILED` 이력이 남고, 사람이 원인을 확인하기 전까지 재시도를 막습니다.

### Docker Compose로 실행 (권장)

`docker compose up`을 실행하면 `migrator` 서비스가 먼저 실행됩니다.

- 기본 모드는 `auto`입니다. `migrate`를 시도하고, DB가 없으면 `init`을 실행합니다.

```bash
# 명시적으로 실행
docker compose run --rm migrator migrate
docker compose run --rm migrator init

# 기존 DB 편입(adopt) — compose 환경변수를 그대로 사용
docker compose run --rm --entrypoint sh migrator -c \
  '/opt/haru-migrator/bin/haru-database-migrations adopt --url "$HARU_DB_URL" --database "$HARU_DATABASE" --user "$HARU_DB_USER" --version 0'
```

### 직접 실행

`sql/`에는 Gradle Wrapper가 없으므로 시스템 Gradle(8.x)이 필요합니다.

```bash
gradle -p sql installDist
HARU_DB_PASSWORD='<비밀번호>' sql/build/install/haru-database-migrations/bin/haru-database-migrations \
  migrate --url 'jdbc:mysql://localhost:3306/?allowPublicKeyRetrieval=true&useSSL=false' --database haru_db --user root
```

- 저장소 루트에서 실행해야 `sql/schema.sql`을 찾습니다.
- 다른 위치에서 실행하려면 `HARU_SQL_DIR`로 `sql/` 경로를 지정합니다.

## 로컬 시드 데이터

러너로 스키마를 만든 뒤 시드를 넣습니다. 모든 INSERT는 `INSERT IGNORE`라서 다시 실행해도 안전합니다.

```bash
# Docker Compose
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < sql/seed-local.sql

# 로컬 MySQL
mysql -u root -p < sql/seed-local.sql
```

| 테스트 계정 | 비밀번호 |
|---|---|
| `test1@haru.com` | `Haru!local1` |
| `test2@haru.com` | `Haru!local1` |

> 시드 계정은 **로컬 전용**입니다. 운영 DB에는 넣지 마세요.

## 통합 테스트용 DB

CoreService와 `sql/`의 MySQL 통합 테스트는 지정한 서버에 임시 DB(`haru_r02_<uuid>`, `haru_r03_<uuid>`)를 만들고, 테스트가 끝나면 삭제합니다.

- 반드시 **테스트 전용 MySQL 서버**를 지정하세요.
- 실행 방법은 [개발 가이드](development.md#테스트)를 참고하세요.

## 레거시 자료: `MySQL/`

루트의 `MySQL/` 디렉터리는 팀 프로젝트 초기의 테이블 설계 노트(`.txt`)입니다.

- **현재 스키마의 기준이 아닙니다.** 구세대 정의(`Posts`, `boards`)와, 구현되지 않은 설계(`ServicePricing`, `Reviews`, `Reports` 등)가 섞여 있습니다.
- 실제 스키마는 위의 `sql/`을 따릅니다.
