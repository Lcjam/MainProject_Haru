# Haru 백엔드 API 반복 점검

저장된 컬렉션: [Haru Backend API Checks](https://web.postman.co/workspace/76b84b48-8e3b-4c09-89ed-e49e590dd7c5/collection/42109323-b435dbce-2e09-4a9f-8706-08747836520c). 개인 워크스페이스 로그인 후 연다.

`haru.postman_collection.json`을 Postman에서 실행하거나 Newman 명령으로 반복 실행한다. 기본 주소는 Gateway `http://127.0.0.1:8080`이다. 서버·MySQL·Redis를 먼저 기동한다. 이 컬렉션의 인증 거부 테스트가 모든 REST 경로를 다루더라도 전체 기능의 정상 동작을 보증하지는 않는다.

| 폴더 | 확인하는 내용 | 필요한 설정 |
|---|---|---|
| `01 Startup` | 공개 조회 응답·로그인 입력 검증·인증 누락 거부, 공유 오류 계약(`BAD_REQUEST`·`INVALID_PATH`·`NOT_FOUND`) | 없음 |
| `02 Authenticated` | 실제 로그인·프로필·취미·게시판·채팅·상품·거래 조회, Assist 입력 검증 | 로컬 환경의 `email`, `password` |
| `03 Route auth` | 현재 Core 100개·Assist 7개 컨트롤러 REST 매핑마다 잘못된 토큰의 `401 / AUTH_INVALID` 응답 | 없음. Gateway 검사이며 downstream 기동 증거가 아님 |
| `04 Board CRUD` | 이번 실행에서 만든 게시판·게시글·댓글 생성·조회·수정·삭제 | 전용 테스트 계정과 `enableCrud=true` |

## Postman에서 실행

1. `Haru Backend API Checks` 컬렉션을 열거나 JSON을 Import한다.
2. 기동 점검은 `01 Startup`, `03 Route auth` 폴더를 선택해 Run한다. 로컬 서버에 접근할 수 있는 Desktop 앱/에이전트를 사용한다.
3. 로그인 조회는 로컬 환경에 `email`, `password`를 입력하고 `02 Authenticated`를 실행한다. 계정값을 shared value에 저장하지 않는다. 토큰은 스크립트가 실행 범위 변수에만 보관한다.
4. CRUD는 전용 테스트 계정으로 로컬 환경의 `enableCrud`를 `true`로 설정하고 `04 Board CRUD`를 실행한다. 기본값 `false`에서는 이 폴더를 건너뛴다. 결과의 건너뜀을 통과 건수로 세지 않는다.

계정값 없는 상태에서 `02 Authenticated`를 실행하면 실패한다. 이는 로그인 조회를 검증하지 않은 상태가 성공으로 표시되는 것을 막기 위한 동작이다. 반복 Run에서는 이전 실행의 토큰·리소스 ID를 재사용하지 않는다.

## 터미널에서 기동 점검

저장소 루트에서 실행한다. Node.js/npm과 인터넷 연결이 최초 Newman 다운로드에 필요하다. 저장소의 package.json이나 lockfile은 변경하지 않는다.

```bash
npx --yes newman@6.2.3 run api-tests/haru.postman_collection.json \
  --folder "01 Startup" --folder "03 Route auth" --timeout-request 5000
```

실패 시 종료 코드가 0이 아니므로 기동 스크립트나 CI의 다음 명령 앞에 이 명령을 연결할 수 있다. 기존 기동 스크립트에는 자동 삽입하지 않았다. 다른 주소는 `--env-var baseUrl=http://localhost:8080`으로 지정한다.

인증 조회에는 `api-tests/dev.local.postman_environment.json`을 로컬에서 만든다. 아래 값은 실행 전에 전용 테스트 계정으로 채운다. 파일은 Git에서 제외되며 Postman에 업로드할 필요가 없다.

```json
{
  "name": "Haru local",
  "values": [
    {"key": "email", "value": "", "enabled": true},
    {"key": "password", "value": "", "enabled": true},
    {"key": "enableCrud", "value": "false", "enabled": true}
  ]
}
```

```bash
npx --yes newman@6.2.3 run api-tests/haru.postman_collection.json \
  --folder "02 Authenticated" -e api-tests/dev.local.postman_environment.json \
  --timeout-request 5000
```

CRUD는 같은 파일의 `enableCrud`를 `true`로 바꾸고 폴더를 지정한다. 해당 폴더는 자체 로그인부터 실행한다.

```bash
npx --yes newman@6.2.3 run api-tests/haru.postman_collection.json \
  --folder "04 Board CRUD" -e api-tests/dev.local.postman_environment.json \
  --timeout-request 5000
```

기존 로컬 seed 계정이 필요하면 `sql/seed-local.sql`을 적용한다. 이 테스트는 계정 생성이나 DB 초기화를 자동으로 수행하지 않는다.

## 해석과 한계

- `03 Route auth`는 Gateway에서 요청이 끝난다. Core·Assist가 내려가 있어도 이 폴더만 통과할 수 있다. `01 Startup`과 함께 실행하고, Assist 자체 응답은 `02 Authenticated`로 확인한다.
- 기본 점검은 기존 업무 데이터를 변경하지 않는다. 로그인은 토큰 발급을 수행한다. CRUD는 해당 실행에서 받은 ID만 사용하지만 중단·서버 오류 시 테스트 데이터가 남을 수 있다. 데이터가 중요한 환경에서는 전용 DB를 사용한다.
- 게시판 삭제는 현재 서버의 소프트 삭제 정책을 따른다. 정상 종료해도 해당 DB 행은 남으며, 이 컬렉션은 물리 삭제나 DB 초기화를 수행하지 않는다.
- SMS 실발송, OCR·주소검색·번역·AI 실연동, OAuth 제공자 왕복, 파일 업로드·지속성, STOMP, 상품 참여·결제의 정상 시나리오는 이 컬렉션의 기능 검증 범위 밖이다. 해당 REST 경로는 인증 거부 검증에만 포함된다. 기존 자동 테스트와 함께 사용한다.
- HTTP 200뿐 아니라 성공 상태·응답 형태를 검사한다. 기동 직후 연결 거부/5xx가 발생하면 서비스 준비 상태와 로그를 확인하고 다시 실행한다.

## API 추가 후 갱신

```bash
node api-tests/generate.mjs
node api-tests/verify.mjs
```

생성기는 현재 Java 컨트롤러의 REST 매핑으로 컬렉션의 경로 인증 검사를 재생성한다. 현재처럼 경로를 한 줄에 쓰는 매핑 어노테이션을 전제로 한다. 여러 줄·배열·새 매핑 형식을 도입하면 생성기를 먼저 확장해야 한다. 새 기능의 정상 시나리오가 자동으로 생기는 것은 아니다. Postman에서 수정한 내용은 재생성 전에 생성기에 반영한다.

MCP 일괄 저장 후에는 개별 요청의 스크립트·Bearer 설정이 유지되는지 재조회해야 한다. 이번 저장에서는 일괄 저장 후 누락된 설정을 요청별 갱신으로 보완하고, 146개 요청의 URL·헤더·본문·인증·스크립트와 폴더 공통 스크립트를 로컬 파일과 대조했다. 이후 파일을 수정하면 Postman에도 다시 반영해야 한다. C01(공유 오류 계약)에서 요청이 149개로 늘었으므로 Postman 저장본은 JSON을 다시 Import해 갱신한다.

실행 옵션과 건너뛰기 동작은 [Newman 공식 문서](https://learning.postman.com/docs/reference/newman-cli/newman-options), [Postman 실행 제어 문서](https://learning.postman.com/v11/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-execution)를 참조한다.
