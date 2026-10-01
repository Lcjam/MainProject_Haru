# API 레퍼런스

프론트엔드는 모든 요청을 게이트웨이(`http://localhost:8080`)로 보냅니다. 아래 경로는 게이트웨이를 거친 뒤의 실제 서비스 경로입니다.

- 인증이 필요한 요청에는 `Authorization: Bearer <JWT>` 헤더를 붙입니다.
- **공개** 표시가 없는 경로는 모두 JWT가 필요합니다. 공개 경로를 정하는 기준은 [아키텍처 문서](architecture.md#2단계-인가)를 참고하세요.
- 요청·응답 필드의 상세 스키마는 Swagger UI에서 확인할 수 있습니다.
  - CoreService(직접 실행 시): `http://localhost:8081/swagger-ui.html`
  - AssistService(직접 실행 시): `http://localhost:8082/swagger-ui.html`

## 응답 형식

CoreService에는 응답 봉투가 두 종류 있습니다.

| 봉투 | 사용 도메인 | 형태 |
|---|---|---|
| `ApiResponse` | 인증·프로필·취미·게시판·채팅·위치 | `{ "status": "success" \| "error", "data": ..., "code": "200" }` |
| `BaseResponse` | 마켓(상품·거래·결제·사용자 위치) | `{ "status": "success" \| "error", "message": "...", "data": ... }` |

처리되지 않은 예외는 `GlobalExceptionHandler`가 `ApiResponse` 형식으로 바꿉니다(400/401/403/404/500).

---

## CoreService

### 인증 `/api/core/auth`

| 메서드 | 경로 | 설명 | 공개 |
|---|---|---|---|
| POST | `/api/core/auth/signup` | 회원가입 (이메일·닉네임·전화번호 중복 확인, 취미-카테고리 검증) | ✅ |
| POST | `/api/core/auth/login` | 로그인. JWT 발급, 실패 5회 누적 시 계정 잠금 | ✅ |
| POST | `/api/core/auth/logout` | 로그아웃. 토큰을 블랙리스트에 등록 | ✅ |
| PUT | `/api/core/auth/me/password` | 비밀번호 변경 | |
| POST | `/api/core/auth/me/withdrawal` | 회원 탈퇴 (소프트 삭제) | |
| GET | `/api/core/auth/oauth2/{google\|naver\|kakao}` | 소셜 로그인 시작 (리다이렉트) | ✅ |
| GET | `/api/core/auth/oauth2/status` | 지원하는 소셜 로그인 제공자 목록 | ✅ |

### 프로필 `/api/core/profiles`

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/core/profiles/me` | 내 프로필 |
| PUT | `/api/core/profiles/me` | 프로필 수정 |
| PUT | `/api/core/profiles/me/password` | 비밀번호 변경 |
| POST / DELETE | `/api/core/profiles/me/image` | 프로필 이미지 업로드 / 삭제 |
| GET | `/api/core/profiles/me/image-info` | 내 프로필 이미지 정보 |
| GET | `/api/core/profiles/image/{filename}` | 프로필 이미지 파일 |
| GET | `/api/core/profiles/image/default` | 기본 프로필 이미지 |
| GET | `/api/core/profiles/user/{nickname}` | 다른 사용자의 공개 프로필 |
| GET | `/api/core/profiles/mypage` | 마이페이지 요약 |
| GET | `/api/core/profiles/admin/user/{email}` | 사용자 조회 (`ROLE_ADMIN`) |

### 취미 `/api/core/hobbies`

| 메서드 | 경로 | 설명 | 공개 |
|---|---|---|---|
| GET | `/api/core/hobbies` | 전체 취미 | ✅ |
| GET | `/api/core/hobbies/simple` | 취미 목록 (간략) | ✅ |
| GET | `/api/core/hobbies/categories` | 카테고리 목록 | ✅ |
| GET | `/api/core/hobbies/categories/{categoryId}` | 카테고리별 취미 | ✅ |
| GET / POST | `/api/core/hobbies/user` | 내 취미 조회 / 등록·수정 | |

### 게시판(모임) `/api/core/boards`

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/core/boards` | 게시판 생성 |
| GET / PUT / DELETE | `/api/core/boards/{boardId}` | 게시판 조회 / 수정 / 삭제 |
| POST | `/api/core/boards/{boardId}/image` | 게시판 대표 이미지 업로드 |
| GET | `/api/core/boards/hosted` | 내가 만든 게시판 |
| GET | `/api/core/boards/joined` | 내가 가입한 게시판 |
| GET | `/api/core/boards/{boardId}/members` | 멤버 목록 |
| POST | `/api/core/boards/{boardId}/members/{invite\|accept\|reject}` | 멤버 초대 / 초대 수락 / 거절 |
| DELETE | `/api/core/boards/{boardId}/members/{memberId}` | 멤버 강퇴 |
| PUT | `/api/core/boards/{boardId}/status` | 게시판 상태 변경 |
| PUT | `/api/core/boards/{boardId}/host` | 방장 위임 |

### 게시글·댓글·반응

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET / POST | `/api/core/boards/{boardId}/posts` | 게시글 목록 / 작성 |
| GET / PUT / DELETE | `/api/core/boards/{boardId}/posts/{postId}` | 게시글 조회 / 수정 / 삭제 |
| GET / POST | `/api/core/boards/{boardId}/posts/search` | 게시글 검색 (POST는 필터 조건 포함) |
| POST / DELETE | `/api/core/boards/{boardId}/posts/images` | 게시글 이미지 업로드 / 삭제 |
| GET | `/api/core/boards/images/{boardId}/{fileType}/{fileName}` | 게시판 첨부 파일 조회 |
| GET / POST | `/api/core/boards/posts/{postId}/comments` | 댓글 목록 / 작성 |
| PUT / DELETE | `/api/core/boards/posts/{postId}/comments/{commentId}` | 댓글 수정 / 삭제 |
| GET / POST | `/api/core/boards/posts/{postId}/comments/{commentId}/replies` | 대댓글 목록 / 작성 |
| GET / POST / DELETE | `/api/core/boards/posts/{postId}/reactions` | 내 반응 조회 / 추가 / 삭제 |
| GET | `/api/core/boards/posts/{postId}/reactions/list` | 게시글 반응 목록 |
| POST | `/api/core/boards/{postId}/like` | 좋아요 토글 |

### 마켓: 상품 `/api/core/market/products`

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/core/market/products/registers` | 상품 등록 (`multipart/form-data`: `request` JSON + `images`) |
| GET | `/api/core/market/products/all` | 모집 중인 상품 목록 |
| POST | `/api/core/market/products/all/filter` | 카테고리 필터 + 가격순/최신순 정렬 |
| POST | `/api/core/market/products/nearby?distance=` | 내 최근 위치 기준 반경 내 상품 |
| GET | `/api/core/market/products/{id}` | 상품 상세 (이미지 포함) |
| GET | `/api/core/market/products/images/{imageId}` | 상품 이미지 파일 |
| POST | `/api/core/market/products/users` | 내 상품 목록 (요청 본문의 `types`로 구분) |
| GET | `/api/core/market/products/users/{registers\|requests}/{buy\|sell}` | 내가 등록한 / 요청한 구매·판매 상품 |
| POST | `/api/core/market/products/requests/with-chat` | 상품 신청 + 채팅방 생성 + 등록자 알림 |
| POST | `/api/core/market/products/requests` | 위와 같은 처리 (별칭 경로) |
| POST | `/api/core/market/products/requests/approve` | 신청 승인 (등록자만). 승인 시 거래가 생성됨 |
| POST | `/api/core/market/products/requests/approved` | 상품의 승인된 신청 목록 |
| GET | `/api/core/market/products/requests/approval-status?productId=&requestEmail=` | 신청 승인 상태 |

### 마켓: 거래·결제 `/api/core/market/transactions`

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/core/market/transactions` | 승인된 신청으로 거래 생성 (등록자만, 이미 있으면 기존 거래 반환) |
| GET | `/api/core/market/transactions/{id}` | 거래 조회 (참여자만, 그 외 404) |
| GET | `/api/core/market/transactions/user` | 내 거래 목록 |
| POST | `/api/core/market/transactions/{id}/cancel` | 결제 전 거래 취소 |
| POST | `/api/core/market/transactions/payments` | 결제 (구매자만, 금액은 서버가 계산) |
| GET | `/api/core/market/transactions/payments/{transactionId}` | 거래의 결제 내역 |

### 마켓: 사용자 위치 `/api/core/market/users`

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/core/market/users/location` | 내 위치 갱신 |
| GET | `/api/core/market/users/location/latest` | 내 최신 위치 |

### 채팅 `/api/core/chat`

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/core/chat/rooms` | 상품 기반 채팅방 생성 또는 기존 방 조회 |
| GET | `/api/core/chat/rooms` | 내 채팅방 목록 |
| GET | `/api/core/chat/rooms/active` | 모집 중이거나 승인된 채팅방 |
| GET | `/api/core/chat/rooms/{chatroomId}` | 채팅방 상세 |
| GET | `/api/core/chat/rooms/product/{productId}` | 상품 ID로 내 채팅방 ID 조회 |
| POST | `/api/core/chat/rooms/{chatroomId}/approve` | 채팅방에서 신청 승인 (등록자만) |
| GET | `/api/core/chat/rooms/{chatroomId}/messages` | 메시지 이력 (페이지네이션) |
| PUT | `/api/core/chat/rooms/{chatroomId}/messages/read` | 읽음 처리 |
| POST | `/api/core/chat/messages` | 메시지 전송 (REST) |
| POST | `/api/core/chat/messages/image` | 이미지 메시지 전송 |

### 채팅방 위치 공유 `/api/core/location`

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/core/location/rooms/{chatroomId}/recent` | 채팅방의 최근 위치들 |
| GET | `/api/core/location/rooms/{chatroomId}/users/{email}/last` | 특정 참여자의 마지막 위치 |

### WebSocket (STOMP)

| 방향 | 목적지 | 설명 |
|---|---|---|
| 연결 | `/ws` | CONNECT 프레임의 `Authorization` 헤더에 JWT 필요 |
| 발행 | `/app/chat/send` | 채팅 메시지 전송 (본문에 `chatroomId`) |
| 발행 | `/app/location/{chatroomId}` | 실시간 위치 전송 |
| 구독 | `/topic/room.{chatroomId}`, `/topic/chat.{chatroomId}` | 채팅방 메시지 |
| 구독 | `/topic/location.{chatroomId}` | 채팅방 위치 |
| 구독 | `/topic/user/{email}` | 내 알림 |

---

## AssistService

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/assist/cloudocr/process` | 네이버 클라우드 OCR (이미지 → 텍스트) |
| POST | `/api/assist/sms/send-sms` | SMS 인증번호 발송 |
| POST | `/api/assist/sms/verify-otp` | SMS 인증번호 확인 |
| GET | `/api/assist/location/search` | 주소 검색 (카카오) |
| GET | `/api/assist/location/coord-to-address` | 좌표 → 주소 변환 (카카오) |
| POST | `/api/assist/tinylamanaver/chat` | AI 챗봇 (대화 이력 + 번역 + FastAPI 호출) |
| POST | `/api/assist/upload/profile` | 프로필 이미지 업로드 |

## FastAPI (선택)

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/fastapi/chat` | TinyLlama 기반 고객지원 챗봇. AssistService가 게이트웨이를 거쳐 호출 |

> `/api/fastapi/**`에는 게이트웨이 JWT 필터가 걸려 있지 않습니다. 이유는 [아키텍처 문서의 알려진 제약](architecture.md#알려진-제약)을 참고하세요.
