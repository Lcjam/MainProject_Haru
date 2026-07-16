# CLAUDE.md — Haru 프로젝트

취미 공유 + 중고마켓 + 실시간 채팅 + 위치기반 + AI 챗봇을 갖춘 **마이크로서비스** 앱. 프론트는 게이트웨이만 바라본다.

## 서비스 / 포트
| 서비스 | 포트 | 스택 | 역할 |
|---|---|---|---|
| GateWay | 8080 | Spring Cloud Gateway(WebFlux) + JWT 필터 | 진입점, 인증, 라우팅 |
| CoreService | 8081 | Spring Boot + MyBatis + Redis + STOMP + OAuth2 | 사용자/마켓/보드/채팅/위치/결제 (엔드포인트 80+) |
| AssistService | 8082 | Spring Boot + MyBatis | OCR/SMS/번역/위치/AI챗봇 보조 |
| FastAPI | 8001 | FastAPI + TinyLlama | AI 챗봇 (로컬 기동 범위 외) |
| Frontend | 3000 | Vite + React 19 + TS + Redux + STOMP + Leaflet + opencv | 웹 UI |
| MySQL `haru_db` | 3306 | — | 영속 데이터 (40+ 테이블, 5도메인) |
| Redis | 6379 | — | 세션/캐시, CoreService 전용 |

요청 흐름: **프론트(3000) → GateWay(8080) → Core(8081)/Assist(8082)**. WS: 프론트 → `ws://localhost:8080/ws` → Gateway → Core.

## 자주 쓰는 작업 = 스킬로 등록됨
- `/haru-run` — 전 스택 기동 + 헬스체크 + 종료
- `/haru-db` — MySQL `haru_db` 접속(자격증명은 `.env`에서) + 스키마
- `/haru-verify` — 서버 없이 빌드/컴파일 검증 + 리팩토링 지표 수집

상세 절차는 위 스킬 참조. 빠른 헬스체크: `POST /api/core/auth/login {}` → 400 JSON이면 게이트웨이→Core→DB 왕복 정상.

## 환경 / 사전조건 (중요 함정)
- **JDK 17 toolchain**: 3개 Spring 서비스가 `build.gradle`에 Java 17 고정. 머신엔 JDK 21만 있으면 빌드 실패 → `~/.gradle/gradle.properties`에 `org.gradle.java.installations.paths=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` 등록(이미 적용됨). LOCAL_DEV.md의 "21 자동처리" 설명은 사실과 다름.
- **DB 자격증명**: `CoreService/.env` / `AssistService/.env`의 `DB_PASSWORD`. **하드코딩·커밋 금지**, 항상 `.env`에서 읽을 것.
- **AssistService SMS**: 빈 `SMS_API_URL`이면 nurigo가 기동 크래시 → `SmsService`에서 `https://api.solapi.com` 폴백(적용됨).
- **Assist 채팅 테이블**: 매퍼는 `chat_messages` 사용(과거 `service_chat_messages` 불일치 수정됨). 테이블은 `haru_db`에 생성됨.
- **`productrequests` 테이블 필수**: "함께하기" 요청 테이블. ProductMapper/ProductRequestMapper/ChatRoomMapper가 전반 참조 → 없으면 채팅방 목록/상세가 `MySQL 1146` → **HTTP 500**(채팅방 0개 유저도 동일). 리포에 스키마 파일이 없어(`.gitignore`가 `*.sql` 제외) 누락되기 쉬움. 재생성 DDL: `CoreService/sql/productrequests.sql`(git 미추적) → `mysql -u root haru_db < CoreService/sql/productrequests.sql`.

## 코드 규약 / 진행 중 작업
- DB 테이블 케이싱 혼재(`Users` vs `boards`) — 물리 리네임은 위험, 코드 레벨 정합으로만.
- 응답 래퍼가 `BaseResponse`/`ApiResponse`로 혼재 → **하나로 통일 진행 중**. 신규 코드는 표준 래퍼 + `@RestControllerAdvice` 사용.
- 디버그는 `System.out.println`/`console.log` 금지, SLF4J/logger 사용.
- 토큰 추출은 `CoreService/.../util/TokenUtils.extractTokenWithoutBearer()` 단일 소스 사용.
- 시크릿(JWT secret, OAuth redirect, 호스트)은 하드코딩 금지, 프로퍼티/env로.

## 작업 방식 (리팩토링·멀티모듈)
- 여러 서비스/모듈에 걸치고 병렬화 가능한 작업은 **역할별 서브에이전트로 분담**한다.
  역할은 `.claude/agents/`의 기존 에이전트를 `subagent_type`으로 호출:
  Core/Assist=`spring-boot-engineer`, Frontend=`react-specialist`/`typescript-pro`,
  WS=`websocket-engineer`, DB=`sql-pro`, 횡단=`refactoring-specialist`/`test-automator`/`code-reviewer`.
  사소·단일파일 변경은 스폰 없이 직접(스폰은 비용↑).
- 이 에이전트들은 범용이라 Haru 맥락을 모른다. **spawn 프롬프트에 반드시 주입**:
  대상 모듈 경로·함정(JDK17 toolchain·`.env`·`productrequests`)·**"자기 모듈만, 커밋 금지"**.
- 각 서브에이전트는 자기 모듈만 편집·테스트하고 **커밋하지 않는다** — 리드가 diff 리뷰 후
  작은 커밋으로 순차 정리(git 레이스 방지).
- **통합 검증 (전원 종료 후 리드가 직접):** ①변경 모듈 테스트 직접 재실행
  (`./gradlew cleanTest test`/`npm test`, 보고만 신뢰 금지) ②모듈 경계는 `/haru-run`으로
  부팅·로그인400·보호401·프론트200(필요시 WS 왕복) ③종합해 사용자에게 통합 보고.

## 리팩토링 로드맵
포트폴리오용 리팩토링 진행 중. 계획: `~/.claude/plans/jiggly-jumping-crayon.md`.
산출물: `docs/portfolio/haru-overview.html` (단일 자체완결 HTML, 스냅샷→before/after).
기존 포트폴리오 텍스트: `docs/portfolio/{PORTFOLIO_STAR,PORTFOLIO_PAAR,INTERVIEW_QA,PAAR_BACKEND_HARDENING}.md`.

## 참고 문서
- `LOCAL_DEV.md` — 로컬 구동 가이드(한국어)
- `run-local.sh` — 일괄 기동(프론트 포그라운드)
- `.codex/rules/` — 단계별(planning/implementation/testing/review/release) 작업 규칙
