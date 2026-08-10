package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * S2(AssistService 신원 위조) 회귀 방지.
 *
 * <p>수정 전 {@code getEmailFromToken()} 은 서명 검증 없이 페이로드만 Base64 디코딩해
 * {@code sub} 를 읽었다. 아무나 만든 토큰으로 임의 사용자를 사칭할 수 있었고,
 * <b>이 테스트가 그 동작을 "성공"으로 단언해 취약점을 고정</b>하고 있었다.
 * 수정과 함께 해당 단언을 뒤집는다.
 */
@DisplayName("AssistService TokenUtils — JWT 서명 검증")
class TokenUtilsTest {

    /** application.properties 의 로컬 개발용 기본값. GateWay/CoreService 와 동일. */
    private static final String LOCAL_DEV_SECRET =
            "haru-local-dev-insecure-jwt-secret-change-in-prod-please";

    /**
     * <b>CoreService {@code JwtTokenProvider.createToken()} 이 실제로 발급한 토큰</b>
     * (sub=cross-service@haru.com, 만료 2126년).
     *
     * <p>키 파생 불일치 검출용 교차 검증 fixture. Core·GateWay 는 시크릿을 Base64 인코딩한
     * <i>문자열의 바이트</i>로 HMAC 키를 만든다(이중 인코딩). Assist 가 표준 파생을 쓰면
     * 같은 시크릿인데도 이 토큰의 검증이 실패한다 — Assist 자체 생성 토큰만으로 테스트하면
     * 양쪽이 함께 틀려도 통과하므로 잡히지 않는다.
     *
     * <p>동일한 상수가 CoreService {@code JwtCrossServiceContractTest} 에도 있다.
     * Core 쪽 파생이 바뀌면 그쪽 테스트가 먼저 깨진다. <b>두 상수는 항상 같아야 한다.</b>
     */
    private static final String CORE_ISSUED_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjcm9zcy1zZXJ2aWNlQGhhcnUuY29tIiwidXNlcklkIjoxLCJyb2xl"
                    + "cyI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzg2MzUyMTc1LCJleHAiOjQ5Mzk5NTIxNzV9"
                    + ".0d0MdEcCmZgFMCiuOWPCScx1afDEKoIzwGvC4Y-UaNs";

    private final TokenUtils tokenUtils = new TokenUtils(LOCAL_DEV_SECRET);

    // --- extractTokenWithoutBearer ---------------------------------------

    @Test
    void extractTokenWithoutBearer_stripsPrefix() {
        assertEquals("abc", tokenUtils.extractTokenWithoutBearer("Bearer abc"));
    }

    @Test
    void extractTokenWithoutBearer_noPrefixUnchanged() {
        assertEquals("abc", tokenUtils.extractTokenWithoutBearer("abc"));
    }

    @Test
    void extractTokenWithoutBearer_nullReturnsNull() {
        assertNull(tokenUtils.extractTokenWithoutBearer(null));
    }

    @Test
    void extractTokenWithoutBearer_prefixOnlyNotGlobalReplace() {
        // "Bearer "가 접두사가 아니면 변경 없음
        assertEquals("xBearer y", tokenUtils.extractTokenWithoutBearer("xBearer y"));
    }

    // --- getEmailFromToken -----------------------------------------------

    @Test
    @DisplayName("교차 검증: CoreService 가 발급한 실토큰의 sub 를 반환 (키 파생 일치)")
    void getEmailFromToken_acceptsCoreIssuedToken() {
        assertEquals("cross-service@haru.com", tokenUtils.getEmailFromToken(CORE_ISSUED_TOKEN));
    }

    @Test
    @DisplayName("교차 검증: Bearer 접두사가 붙어도 동일하게 검증")
    void getEmailFromToken_acceptsBearerPrefixedToken() {
        assertEquals(
                "cross-service@haru.com",
                tokenUtils.getEmailFromToken("Bearer " + CORE_ISSUED_TOKEN));
    }

    @Test
    @DisplayName("위조 서명 토큰: null — S2 회귀 방지 (수정 전에는 sub 를 그대로 반환했다)")
    void getEmailFromToken_forgedSignatureReturnsNull() {
        String payload = "{\"sub\":\"attacker@evil.com\"}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        String forgedJwt = "eyJhbGciOiJIUzI1NiJ9" + "." + encodedPayload + "." + "sig";

        assertNull(tokenUtils.getEmailFromToken(forgedJwt));
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰: null")
    void getEmailFromToken_wrongSecretReturnsNull() {
        TokenUtils otherService = new TokenUtils("completely-different-secret-value-for-hmac-key-x");

        assertNull(otherService.getEmailFromToken(CORE_ISSUED_TOKEN));
    }

    @Test
    void getEmailFromToken_malformedReturnsNull() {
        // 청크가 2개뿐 -> null
        assertNull(tokenUtils.getEmailFromToken("not.a"));
    }

    @Test
    void getEmailFromToken_nullOrBlankReturnsNull() {
        assertNull(tokenUtils.getEmailFromToken(null));
        assertNull(tokenUtils.getEmailFromToken("   "));
    }
}
