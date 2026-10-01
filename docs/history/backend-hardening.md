# 백엔드 하드닝 기록

인증·인가와 데이터 정합성을 보강한 변경을 **문제 → 변경 → 이유 → 영향** 순서로 정리했습니다.

> 일부 "변경 전" 코드는 저장소 히스토리 이전의 동작을 줄여서 다시 쓴 예시입니다. 파일 전체를 인용한 것이 아닙니다.

- [1부 기준선 이전 하드닝](#1부-기준선-이전-하드닝): 팀 프로젝트 마무리 단계, 기준선 `95859d9` 이전
- [2부 2026-08 보안 결함 수정](#2부-2026-08-보안-결함-수정): S1, S2
- 이후의 STOMP 구독 인가(S02)와 거래·결제(D04)는 [개선 이력](changelog.md#2026-09--db-마이그레이션-통합-테스트-거래-흐름)과 [PR #3](https://github.com/Lcjam/MainProject_Haru/pull/3)을 참고하세요.

---

## 1부 기준선 이전 하드닝

### 1. 토큰 없는 비밀번호 변경 경로 제거

**문제**: 비밀번호 변경 API가 토큰 없는 경로(`/me/password/notoken`)로도 열려 있었습니다. 인증 없이 비밀번호 변경 로직에 닿을 수 있었습니다.

```java
// 변경 전
@PutMapping("/me/password/notoken")
public ResponseEntity<PasswordChangeResponse> changePasswordWithoutToken(
        @RequestBody PasswordChangeRequest request) {
    return ...userService.changePassword(request); // 요청 본문의 email 기준
}

// 변경 후 — AuthController
@PutMapping("/me/password")
public ResponseEntity<PasswordChangeResponse> changePassword(
        @RequestHeader("Authorization") String token,
        @RequestBody PasswordChangeRequest request) {
    PasswordChangeResponse response = userService.changePasswordByToken(token, request);
    ...
}
```

**영향**: 비밀번호 변경은 인증된 본인만 할 수 있습니다. 클라이언트는 `Authorization` 헤더를 붙여 `/api/core/auth/me/password`를 호출해야 합니다.

### 2. 비밀번호 변경 경로를 `permitAll`에서 제외

**문제**: 비밀번호 변경 경로가 `SecurityConfig`의 `permitAll()` 목록에 섞여 있었습니다. 컨트롤러 코드와 상관없이 필터 체인에서 인증이 생략될 수 있었습니다.

```java
// 변경 전
.requestMatchers("/api/core/auth/signup", "/api/core/auth/login",
                 "/api/core/auth/me/password", "/api/core/auth/me/password/notoken").permitAll()

// 변경 후
.requestMatchers("/api/core/auth/signup", "/api/core/auth/login", "/api/core/auth/logout").permitAll()
.requestMatchers("/api/core/auth/me/password").authenticated()
```

**영향**: 공개 경로(가입·로그인)와 계정 보안 경로가 분리되었습니다. 토큰 없는 요청은 401을 받습니다.

### 3. 게이트웨이 공개 경로 축소와 JWT 키 파생 정렬

**문제**

- 게이트웨이 공개 경로에 `/api/core/chat/**`, `/api/core/market/**`처럼 넓은 패턴이 있어, 인증 없이 통과하는 구간이 컸습니다.
- 게이트웨이와 CoreService의 JWT 키 파생 방식이 달라, 한쪽이 발급한 토큰을 다른 쪽이 거부할 수 있었습니다.

```java
// 변경 전
PUBLIC_PATHS = List.of("/api/core/auth/**", "/api/core/chat/**", "/api/core/market/**");
SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(UTF_8));

// 변경 후 — GateWay JwtAuthenticationFilter
PUBLIC_PATHS = List.of("/api/core/auth/signup", "/api/core/auth/login", "/api/core/auth/logout",
                       "/api/core/auth/oauth2/**", "/ws", "/ws/**", "/topic/**");
String encoded = Base64.getEncoder().encodeToString(secret.getBytes(UTF_8));
SecretKey key = Keys.hmacShaKeyFor(encoded.getBytes(UTF_8)); // CoreService JwtTokenProvider와 동일
```

**영향**

- 공개 경로는 로그인·가입·WebSocket 핸드셰이크 등 꼭 필요한 범위로 줄었습니다.
- 이후 취미·카테고리 조회 경로가 빠져서 장애가 생겼고, 공개 경로에 다시 추가했습니다. → [트러블슈팅 기록](troubleshooting-gateway-whitelist.md)

### 4. 회원가입 실패 처리와 롤백 명시

**문제**

- 회원 기본 정보 INSERT 결과를 확인하지 않고 후속 저장(활동 정보, 취미)을 진행했습니다.
- 예외가 나면 catch에서 실패 응답만 돌려주어 일부 데이터가 저장된 채로 남을 수 있었습니다.

```java
// 변경 후 — AuthService.registerUser
int result = userMapper.insertUser(user);
if (result > 0) {
    userMapper.initializeUserActivity(...);
    hobbyService.registerUserHobbies(...);
    return SignupResponse.builder().success(true)...build();
}
log.error("회원가입 실패 - insertUser 결과: {}", result);
return SignupResponse.builder().success(false)...build();

} catch (Exception e) {
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // 예외를 응답으로 바꾸더라도 롤백
    return SignupResponse.builder().success(false)...build();
}
```

**영향**: 첫 단계 실패와 중간 예외가 모두 전체 실패로 처리됩니다. 일부만 저장되는 일이 없습니다.

### 5. 채팅 접근 검증 일원화

#### 5-1. 메시지의 `productId`를 서버에서 결정

클라이언트가 보낸 `productId`를 그대로 저장하면 채팅방과 상품이 어긋날 수 있었습니다. 이제 채팅방에서 값을 가져옵니다.

```java
ChatRoom chatRoom = chatRoomMapper.findChatRoomById(request.getChatroomId(), senderEmail);
validateChatRoomAccess(chatRoom, senderEmail);
ChatMessage message = ChatMessage.builder()
        .chatroomId(request.getChatroomId())
        .productId(chatRoom.getProductId())   // 요청값이 아니라 채팅방 기준
        ...
```

#### 5-2. `validateChatRoomAccess` 공통 가드

메시지 전송·조회·읽음 처리·이미지 업로드가 각자 권한을 검사해 빠지는 곳이 생길 수 있었습니다. 하나의 메서드로 모았습니다. 검사 순서는 다음과 같습니다.

1. 채팅방이 존재하는지
2. 요청자가 참여자인지
3. 상품이 존재하는지
4. 모집이 끝난 상품이면 승인된 요청자인지

#### 5-3. 모집 완료 상품은 승인된 사용자만 채팅

```java
if (!product.isVisible() && userEmail.equals(chatRoom.getRequestEmail())) {
    ProductRequest pr = productRequestMapper.findByProductIdAndRequesterEmail(chatRoom.getProductId(), userEmail);
    if (pr == null || !"승인".equals(pr.getApprovalStatus())) {
        throw new IllegalArgumentException("모집이 완료된 상품은 승인된 사용자만 채팅이 가능합니다.");
    }
}
```

#### 5-4. 이미지 저장 전에 권한 검사

파일을 디스크에 쓴 뒤에 권한을 검사하면, 권한 없는 요청도 파일을 남길 수 있었습니다. 검사를 저장 앞으로 옮겼습니다.

### 6. 채팅 이미지 저장 경로와 서빙 경로 일치

**문제**: 채팅 이미지를 저장하는 경로(`user.dir/uploads/chat-images`)와 정적 리소스로 서빙하는 경로(`src/main/resources/static/chat-images`)가 달라, 업로드 직후 조회가 404였습니다.

```java
// 변경 후 — WebConfig
registry.addResourceHandler("/chat-images/**")
        .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/chat-images/");
```

**영향**: 실행 디렉터리(`user.dir`)가 바뀌면 경로도 바뀝니다. Docker 이미지는 작업 디렉터리를 `/data` 볼륨으로 고정해 이 문제를 피합니다.

---

## 2부 2026-08 보안 결함 수정

### S1 STOMP CONNECT 인증 우회 차단

커밋: `253b861`(인터셉터 분리, 순수 이동), `98d997e`(수정)

**문제**

- `jwtTokenProvider.validateToken()`은 위조·만료 토큰에 예외를 던지지 않고 `false`를 반환합니다.
- 인터셉터는 이 `false`를 `if`로 걸러 `Principal` 설정만 건너뛰고, 메시지는 그대로 통과시켰습니다. **서명이 가짜인 토큰으로도 CONNECT가 수락**되었습니다.
- 게이트웨이가 `/ws/**`를 JWT 필터에서 제외하므로, 이 인터셉터가 WebSocket의 유일한 방어선이었습니다.

```java
// 변경 전
if (jwtTokenProvider.validateToken(token)) {
    accessor.setUser(principal);
}
return message;            // 검증 실패해도 연결 수락

// 변경 후
if (authorization == null || authorization.isEmpty()) {
    return null;           // 헤더 없음 → 연결 거부
}
if (!jwtTokenProvider.validateToken(token)) {
    return null;           // 위조·만료 → 연결 거부
}
accessor.setUser(new StompPrincipal(email));
```

**검증**

- `StompAuthChannelInterceptorTest` 5개 케이스: 유효 / 위조 / 헤더 없음 / 검증 중 예외 / CONNECT 이외 프레임
- 실제 연결로 재현했습니다. 변경 전에는 위조 토큰이 `CONNECTED`, 변경 후에는 거부되었고 정상 토큰은 그대로 연결되었습니다.

### S2 AssistService JWT 서명 검증

커밋: `db07f89`

**문제**

- AssistService의 `TokenUtils.getEmailFromToken()`은 서명을 검증하지 않고 페이로드만 Base64 디코딩해 `sub`를 읽었습니다.
- 그래서 누구나 만든 토큰으로 다른 사용자를 사칭해 AI 챗봇 세션(세션 ID = 이메일)에 접근할 수 있었습니다.

```java
// 변경 전
String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
return new ObjectMapper().readTree(payload).get("sub").asText();

// 변경 후
Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
        .parseClaimsJws(extractTokenWithoutBearer(token)).getBody();
return claims.getSubject();   // 서명 불일치·만료·형식 오류는 모두 null
```

**키 파생 주의점**

- CoreService와 GateWay는 시크릿을 Base64로 인코딩한 **문자열의 바이트**로 HMAC 키를 만듭니다.
- AssistService도 같은 방식을 써야 합니다. 표준 방식(`secret.getBytes()`)으로 만들면 같은 시크릿인데도 모든 토큰이 검증에 실패합니다.
- 이 차이는 AssistService 단독 테스트로는 잡히지 않습니다. 그래서 다음 두 테스트로 고정했습니다.
  - CoreService가 실제로 발급한 토큰을 fixture로 쓰는 Assist `TokenUtilsTest`
  - Core `JwtCrossServiceContractTest`

**검증**

- 기존 테스트 하나는 위조 토큰에서 이메일 추출이 "성공"한다고 단언하고 있었습니다. 즉 취약점을 그대로 고정하고 있었습니다. 이 단언을 `null`로 바꿨습니다.
- 실행 확인: 위조 토큰은 401, CoreService가 발급한 토큰은 200을 받았습니다.
