# SQL 실행과 스키마 이력

JDK 17과 `lower_case_table_names=0`으로 초기화한 Linux MySQL 8.4 서버를 준비한다. 이 저장소는 Docker 컨테이너 `haru-mysql`(호스트 포트 3307)을 쓰고, WSL2 Ubuntu가 클라이언트다. native Windows MySQL은 대소문자 무시 설정(1)이라 실행기가 거부한다. 설정 2의 기존 DB 이행도 검증 범위 밖이다. 저장소 루트의 Gradle wrapper로 독립 `sql` 프로젝트를 실행한다. CoreService 애플리케이션을 시작할 필요는 없다. Gradle·JDBC 의존성은 최초 다운로드가 필요하다.

`schema.sql`은 20개 테이블의 구조 기준선이고 V1은 7개 테이블과 `Users.profile_image_path`를 추가한다. V2는 이메일 고유 키를 가진 `UserLocationLatest`를 추가하고 기존 `UserLocation` 행을 보존한다. V3는 게시판 임시 이미지의 소유권을 기록하는 `board_temp_images`를 추가한다. `init`은 `sql/migrations/`의 모든 `V*.sql`을 버전순으로 적용하므로 현재 초기화 후 도메인 테이블 29개와 실행기 전용 `haru_schema_history` 1개가 존재하고 버전은 3이다. 원본 schema와 이미 적용된 V 파일을 다시 덤프하거나 수정하지 않는다(새 변경은 다음 번호 V 파일로 추가).

## 적용 모드

| 현재 DB | 명령 | 동작 |
|---|---|---|
| DB가 없거나 완전히 비어 있음 | `init` | 기준선과 버전순 migration 적용 |
| 실행기 이력 없는 baseline 스키마 | `adopt --version 0` 다음 `migrate` | 기존 구조 검사·이력 편입 후 이후 버전(V1~V3) 적용 |
| 실행기 이력 없는 baseline+V1 스키마 | `adopt --version 1` 다음 `migrate` | 기존 구조 검사·이력 편입, V1 재실행 안 함 |
| 실행기 이력이 있는 DB | `migrate` | 이력·체크섬·현재 구조 확인 후 새 버전만 적용 |
| 일부 적용·알 수 없는 구조·실패 이력 | 실행 거부 | 백업과 실패 지점을 검토한 뒤 수동 복구 설계 |

`adopt`는 버전을 추측하지 않는다. 지정한 버전과 테이블·컬럼·인덱스·FK 등이 일치해야 하며 도메인 DDL은 실행하지 않는다. 뷰·루틴·트리거·이벤트·CHECK 제약과 지원하지 않는 인덱스/컬럼 속성도 거부한다. 비교 범위는 아래 제한 문법이며 테이블 주석·AUTO_INCREMENT 현재 카운터·물리 컬럼 순서까지 동일함을 보증하지 않는다. `init`은 이력 테이블 하나만 남아 있어도 재실행할 수 없다. 기존 DB에 `schema.sql`을 직접 실행하면 DROP으로 데이터가 삭제될 수 있으므로 실행기를 사용한다.

## 실행 (WSL2)

모든 명령은 저장소 루트(`~/code/haru`) 기준이다. **저장소 루트에는 wrapper가 없으므로** 독립 `sql` 프로젝트는 `./CoreService/gradlew -p sql`로 실행한다(sh 래퍼. `.bat`은 Windows 보존본 전용).

> **compose로 기동하면 이 절차를 손으로 할 필요가 없다.** `migrator` 서비스가 `auto` 모드로 기동하여 먼저 `migrate`를 시도하고, DB가 없을 때만 `init`을 수행한다.
> 아래는 네이티브 기동용 컨테이너 `haru-mysql`(호스트 포트 **3307**)을 대상으로 한다. 자세한 배경은 [`../LOCAL_DEV.md`](../LOCAL_DEV.md). 아래 비밀번호 입력은 명령 기록에 값을 직접 적지 않기 위한 방법이다. 환경변수는 자식 Java 프로세스에 전달되므로 끝나면 제거한다.

```bash
read -r -s -p "MySQL password: " HARU_DB_PASSWORD; printf '
'
export HARU_DB_PASSWORD
./CoreService/gradlew -p sql run --no-daemon   --args="init --url jdbc:mysql://127.0.0.1:3307/ --database haru_db --user root"
unset HARU_DB_PASSWORD
```

서비스의 `.env`에서 바로 읽어도 된다: `export HARU_DB_PASSWORD=$(grep '^DB_PASSWORD=' CoreService/.env | cut -d= -f2-)`.

기존 DB라면 위 `init` 명령 대신 해당 경우의 명령만 실행한다. 각 실행 전에 같은 비밀번호 환경변수가 필요하다.

```bash
./CoreService/gradlew -p sql run --no-daemon   --args="adopt --version 0 --url jdbc:mysql://127.0.0.1:3307/ --database haru_db --user root"
./CoreService/gradlew -p sql run --no-daemon   --args="migrate --url jdbc:mysql://127.0.0.1:3307/ --database haru_db --user root"
```

V1까지 이미 적용했다면 `--version 1`을 사용한다. `adopt`는 `--version 0`과 `1`만 지원한다. 실행기 이력 없이 V2 이후까지 수동으로 적용된 DB는 편입할 수 없으므로, 데이터를 백업하고 새 DB에 `init`한 뒤 옮기거나 별도 복구 절차를 설계한다.


## 입력과 실패 계약

- `--url`: DB 경로 없는 JDBC 서버 주소. 예: `jdbc:mysql://127.0.0.1:3307/`. 서비스의 `DB_URL`은 DB 경로가 있으므로 그대로 복사하지 않는다.
- `--database`: 영문자로 시작하는 영문·숫자·밑줄, 최대 48자. 대상은 별도 인자로 선택한다.
- `--user`, `HARU_DB_PASSWORD`: 사용자와 비밀번호. 비밀번호 CLI 인자·URL 내 자격증명은 받지 않는다. Core/Assist의 `.env`를 자동 로드하지 않는다.
- `--lock-timeout`: 0~300초, 기본 30초. 같은 서버·DB의 실행기는 동일 세션 잠금을 사용한다.
- `HARU_SQL_DIR`: 기본 위치 대신 SQL 폴더를 지정할 때만 사용한다. 검토된 저장소 SQL을 제공해야 한다.

UTF-8 SQL을 메모리에 읽고 줄바꿈을 LF로 정규화한 SHA-256을 기록한다. 파일 변경, 이력의 순서 역전·누락, 중복 숫자 버전, 알 수 없는 이력은 거부한다. 버전 0은 기준선 전용이다. 버전 번호는 문자열 사전순이 아닌 숫자순으로 처리한다.

이력은 `RUNNING → SUCCESS` 또는 `RUNNING → FAILED`다. 스키마 검사까지 통과해야 성공을 기록한다. 연결 장애로 실패 기록 자체가 불가능하면 RUNNING이 남을 수 있고 다음 실행도 차단한다. MySQL DDL은 암묵적으로 커밋되므로 실행기 실패가 이미 적용한 DDL의 롤백을 뜻하지 않는다. 자동 retry/repair/baseline 재실행은 제공하지 않는다. 실패 DB와 이력을 보존하고 백업·실행 파일·부분 적용 내용을 대조해 별도 복구를 설계한다. [MySQL DDL 커밋 규칙](https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html), [세션 잠금](https://dev.mysql.com/doc/refman/8.4/en/locking-functions.html).

현재 실행기는 저장소 baseline/V1~V3 형태의 SQL을 지원하는 제한된 도구다. 지원 범위는 인용된 테이블명으로 작성한 InnoDB/utf8mb4 CREATE TABLE·단일 ALTER TABLE ADD COLUMN과 기준선의 세션 설정이다. 일반 visible BTREE 오름차순 전체 컬럼 인덱스만 지원하고, prefix/함수/내림차순/숨김 인덱스와 generated/숨김 컬럼은 편입하지 않는다. 검증용 `SELECT SLEEP(정수)`도 허용하지만 데이터 migration 기능은 아니다. DROP/RENAME/임의 ALTER, 저장 프로시저·트리거·이벤트, 동적 SQL, 다른 스키마를 지정하는 참조, 서버·계정 설정 변경은 후속 migration에 넣지 않는다. 새 문법이 필요하면 실행 전 검사·구조 비교·테스트를 함께 확장하거나 기성 migration 도구로 전환하는 별도 설계를 한다. 실행기는 신뢰하지 않는 SQL을 안전하게 실행하는 샌드박스가 아니다.

서버의 `lower_case_table_names`가 0이 아니면 변경 전에 거부한다. 잠금은 이 실행기를 사용하는 세션끼리의 협력 잠금이므로 이행 중 다른 클라이언트의 DDL을 중지해야 한다. 기존 DB 편입 전에는 백업과 복구 가능성을 확인한다. 메타데이터 전체를 볼 수 없는 제한 계정이나 운영 서버는 이번 검증 대상이 아니다.

## 선택적 로컬 seed

`seed-local.sql`은 합성 테스트 계정 2개와 참조 데이터를 넣는다. migration 이력에 포함되지 않는다. 기존 파일의 `USE haru_db` 때문에 **격리한 로컬 개발용 haru_db에서만** 적용하며 운영·개인 데이터 DB에는 적용하지 않는다. `INSERT IGNORE`는 중복을 건너뛰지만 기존 값 갱신이나 다른 제약의 정합성 검증을 대신하지 않는다.

컨테이너 안의 MySQL 클라이언트로 stdin 주입하면 UTF-8이 유지된다.

```bash
docker exec -i haru-mysql mysql --default-character-set=utf8mb4 -uroot -p"$HARU_DB_PASSWORD" < sql/seed-local.sql
```

## 검증과 후속 연결

단위·정적 검사는 DB 없이 실행한다.

```bash
./CoreService/gradlew -p sql test --no-daemon
```

MySQL 통합 검사는 `HARU_TEST_MYSQL_URL`, `HARU_TEST_MYSQL_USER`, `HARU_TEST_MYSQL_PASSWORD`를 명시한 **전용 Linux MySQL 서버**에서만 `mysqlIntegrationTest`로 실행한다. URL은 DB 경로 없는 서버 주소다. 테스트는 `haru_r02_` 접두사의 고유 DB를 만들고 자신이 만든 DB만 정리한다. 테스트 계정에 이 DB들의 CREATE/DROP/DDL/DML 권한이 필요하다. 대소문자 호환 검증 서버는 `lower_case_table_names=0`으로 초기화한다. [MySQL 식별자 대소문자 규칙](https://dev.mysql.com/doc/refman/8.4/en/identifier-case-sensitivity.html).

```bash
./CoreService/gradlew -p sql test mysqlIntegrationTest --no-daemon
```

결과는 `sql/build/test-results/test/`, `sql/build/test-results/mysqlIntegrationTest/`와 각각의 `build/reports/tests/`에 생성된다. 설정 누락을 통과·skip으로 취급하지 않는다.

`verify/mapper-smoke.sql`은 과거 수동 SQL fixture이며 전체 MyBatis 실행 증거를 대신하지 않는다. R03이 실제 MyBatis SQL·트랜잭션·fixture 검증을, R04가 compose 기동과 기존 볼륨 업그레이드 연결을 맡는다. S01 정렬의 실 MySQL 검증은 R03 후속이다.

## 변경 규칙

- migration 번호·실행 순서는 AGENTS.md에 따라 메인이 배정한다. 번호 없는 초안은 실행 폴더에 두지 않는다.
- baseline/V1은 보존하고 새 변경은 새 버전 파일에 추가한다. 이미 적용한 SQL의 수정은 체크섬 오류다.
- 매퍼의 테이블 식별자를 물리 스키마 이름에 맞춘다. 물리 테이블 일괄 rename은 별도 ADR과 이행 검증이 필요하다.
- `MySQL/`의 구세대 SQL 및 `CoreService/sql/productrequests.sql`은 신규 환경의 실행 기준이 아니다.
