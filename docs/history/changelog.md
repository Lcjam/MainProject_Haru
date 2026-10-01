# 개선 이력

팀 프로젝트로 구현한 상태를 기준선(`95859d9`, 2026-06-12)으로 잡고, 그 뒤 진행한 백엔드 안정화 작업을 시간순으로 정리했습니다.
각 항목의 자세한 내용은 커밋 메시지와 PR 본문에 있습니다.

> 일부 커밋과 테스트 이름에 붙은 작업 ID(`S1`, `F3`, `R03`, `D01` 등)는 당시 작업 목록의 항목 번호입니다.
> 예: `D01ApprovalIntegrationTest`는 D01 작업의 테스트입니다.

## 2026-06 · 1차 리팩토링 (기능 보존)

| 커밋 | 내용 |
|---|---|
| `26ddb26` | JWT 시크릿·OAuth2 자격증명·CORS 출처를 환경변수로 외부화하고, 입력 검증(`@Valid`)을 활성화 |
| `3bfb984` | 백엔드 `System.out/err.println`을 SLF4J로 전환 |
| `4f412c0` | `@RestControllerAdvice` 전역 예외 핸들러로 에러 응답 표준화 (검증 실패가 401·한글 깨짐으로 나가던 문제 수정) |
| `2423f54` | 인증 DTO 검증 단위 테스트 추가, JUnit Platform 설정 |
| `218ca28`, `b2c823b` | 401 가드를 예외로 바꾸고 `BoardController`의 try/catch 중복 제거 (426줄 → 244줄) |
| `1d7800f`, `ba908ec` | Bearer 토큰 추출을 `TokenUtils` 한 곳으로 통합 |
| `1ec4dce` | 상품 이미지 파일 I/O를 컨트롤러에서 `ProductService`로 이동 |
| `e7b38ab` | 게이트웨이 공개 경로 누락 수정 → [트러블슈팅 기록](troubleshooting-gateway-whitelist.md) |
| `5083293`, `7235a1f`, `f198995` | AI 챗봇 경로 복구: FastAPI 라우트 JWT 필터 제외, 번역 API 빈 자격증명 처리, llama.cpp(gguf) 전환 |
| `455700a`, `87e7de5`, `0bb65b2`, `6800dc5` | 프론트엔드: Vitest 도입, `any` 축소, 큰 컴포넌트 분리, 프로덕션 빌드에서 console 제거 |

당시 측정값 (Before → After):

| 지표 | Before | After |
|---|---|---|
| 하드코딩된 JWT 시크릿 리터럴 | 2 | 0 |
| 백엔드 `System.out/err.println` | 45 | 0 |
| 전역 예외 핸들러 | 0 | 1 |
| 백엔드 테스트 | 0 | 22 |
| 프론트엔드 Vitest 테스트 | 0 | 47 |
| 프론트엔드 `any` 타입 | 45 | 14 |

기준선 이전(팀 프로젝트 마무리 단계)에 반영된 인증·채팅 하드닝은 [백엔드 하드닝 기록](backend-hardening.md#1부-기준선-이전-하드닝)에 정리되어 있습니다.

## 2026-07 · 저장소 정리

| 커밋 | 내용 |
|---|---|
| `fe593a0` | TLS 개인키를 히스토리에서 제거하고 인증서·키 파일 무시 규칙 추가 |
| `f2ed089` | 비밀값과 대용량 산출물(모델 가중치, 빌드 결과)을 추적 대상에서 제외 |

## 2026-08 · 보안 결함 수정과 계층 정리

| 커밋 | 작업 ID | 내용 |
|---|---|---|
| `3b01eb1` | Phase 0 | self-hosted 배포 워크플로 비활성화 (`.github/workflows-disabled/`) |
| `cbe4d1f` | Phase 0 | `haru_db` 스키마 기준선(`sql/schema.sql`)과 로컬 시드 확보 |
| `253b861`, `98d997e` | S1 | STOMP CONNECT 인증 우회 차단 → [상세](backend-hardening.md#s1-stomp-connect-인증-우회-차단) |
| `db07f89` | S2 | AssistService JWT 서명 미검증으로 인한 신원 위조 차단 → [상세](backend-hardening.md#s2-assistservice-jwt-서명-검증) |
| `9d9d80b` | F3 | 매퍼가 참조하지만 DDL이 없던 테이블 7개와 컬럼 1개 복원 (`V1` 마이그레이션) |
| `c140841` | Phase 1a | `NotFoundException`·`ForbiddenException`과 404/403 핸들러 추가 |
| `379cdf3` | Phase 1a | 리팩토링 전 Chat·PostReaction·ChatMessage·Location HTTP 계약 테스트 62건 추가 |
| `46fda11` | Phase 1a | 컨트롤러의 Mapper 직접 주입 4곳을 서비스로 이동 (동작 변경 없음) |
| `98fe576` | Phase 1a | 이동한 다단계 쓰기 메서드에 `@Transactional` 적용 |

## 2026-09 · DB 마이그레이션, 통합 테스트, 거래 흐름

| 커밋 / PR | 작업 ID | 내용 |
|---|---|---|
| `3b822c8` | R02 | 마이그레이션 러너(`init`/`adopt`/`migrate`) 도입. 스키마 검증, 이력 테이블, named lock. 정렬 방향 입력 검증 |
| `fe3c342` | | 어노테이션 매퍼의 테이블 식별자 검증 |
| `5bc002d` | R03 | 실제 MySQL·Redis를 쓰는 통합 테스트 레인 (`mysqlIntegrationTest`, `redisIntegrationTest`) |
| `057cf97` | R04 | Docker Compose 개발 스택 (`compose.yaml`) |
| `106cbb0` | R05 | CI 최소 게이트 (백엔드 3개 서비스 테스트, 프론트엔드 테스트·빌드) |
| `8bc0c85` | R06 | 거래 구매자(`buyer_email`) 매핑 복구 |
| [#2](https://github.com/Lcjam/MainProject_Haru/pull/2) `5cb3d1b` | D01 | 마켓·채팅의 신청 승인 경로를 하나의 트랜잭션 메서드로 통합. 소유자·요청 관계 검증, 멱등 승인, 상품 행 잠금으로 마지막 자리 직렬화, 커밋 후 알림 |
| [#3](https://github.com/Lcjam/MainProject_Haru/pull/3) `b8e2fc1` | S02 | STOMP 연결·구독·발행 권한과 Origin 검사 |
| | D04 | 승인된 신청에 연결된 거래 생성, 구매자 결제, 상태 전이, 타인 접근 차단 |
| | D08 | 채팅방·사용자 위치의 권한·좌표 검증, 최신 위치 테이블(`V2`) 추가 |
