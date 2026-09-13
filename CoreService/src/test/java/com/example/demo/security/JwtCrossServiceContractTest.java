package com.example.demo.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 서비스 간 JWT 서명 계약을 고정한다.
 *
 * <p>Core·GateWay 는 시크릿을 Base64 인코딩한 <i>문자열의 바이트</i>로 HMAC 키를 만든다
 * (이중 인코딩 — 비표준). AssistService {@code TokenUtils} 가 S2 수정에서 이 파생을 복제했다.
 * 표준 파생({@code secret.getBytes()})으로 바꾸면 같은 시크릿인데도 서비스 간 검증이 전부
 * 깨지는데, 그 고장은 각 서비스 단독 테스트로는 잡히지 않는다.
 *
 * <p>이 테스트는 <b>Core 가 실제 발급했던 토큰</b>을 fixture 로 박아 Core 쪽 파생이 바뀌면
 * 즉시 깨지게 한다. 동일한 상수가 AssistService {@code TokenUtilsTest} 에도 있으며
 * 그쪽은 Assist 파생을 고정한다. <b>두 상수는 항상 같아야 한다.</b>
 */
@DisplayName("서비스 간 JWT 서명 계약 (Core ↔ Assist)")
class JwtCrossServiceContractTest {

    /** application.properties 의 로컬 개발용 기본값. GateWay/AssistService 와 동일. */
    private static final String LOCAL_DEV_SECRET =
            "haru-local-dev-insecure-jwt-secret-change-in-prod-please";

    /** {@code JwtTokenProvider.createToken()} 이 발급한 토큰 (sub=cross-service@haru.com, 만료 2126년). */
    private static final String CORE_ISSUED_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjcm9zcy1zZXJ2aWNlQGhhcnUuY29tIiwidXNlcklkIjoxLCJyb2xl"
                    + "cyI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzg2MzUyMTc1LCJleHAiOjQ5Mzk5NTIxNzV9"
                    + ".0d0MdEcCmZgFMCiuOWPCScx1afDEKoIzwGvC4Y-UaNs";

    private final JwtTokenProvider provider =
            new JwtTokenProvider(LOCAL_DEV_SECRET, 24 * 60 * 60 * 1000L);

    @Test
    @DisplayName("fixture 토큰이 현재 키 파생으로 검증된다 (파생 변경 시 여기서 먼저 깨진다)")
    void coreValidatesItsOwnIssuedToken() {
        assertTrue(provider.validateToken(CORE_ISSUED_TOKEN));
        assertEquals("cross-service@haru.com", provider.getUsername(CORE_ISSUED_TOKEN));
    }

    @Test
    @DisplayName("새로 발급한 토큰도 같은 파생으로 검증된다")
    void freshlyIssuedTokenIsValid() {
        String token = provider.createToken(1, "cross-service@haru.com", List.of("ROLE_USER"));

        assertTrue(provider.validateToken(token));
        assertEquals("cross-service@haru.com", provider.getUsername(token));
    }
}
