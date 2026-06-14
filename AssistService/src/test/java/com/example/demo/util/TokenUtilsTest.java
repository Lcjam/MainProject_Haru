package com.example.demo.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenUtilsTest {

    private final TokenUtils tokenUtils = new TokenUtils();

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

    @Test
    void getEmailFromToken_returnsSubClaim() {
        String payload = "{\"sub\":\"user@haru.com\"}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        String fakeJwt = "eyJhbGciOiJIUzI1NiJ9" + "." + encodedPayload + "." + "sig";

        assertEquals("user@haru.com", tokenUtils.getEmailFromToken(fakeJwt));
    }

    @Test
    void getEmailFromToken_malformedReturnsNull() {
        // 청크가 2개뿐 -> null
        assertNull(tokenUtils.getEmailFromToken("not.a"));
    }
}
