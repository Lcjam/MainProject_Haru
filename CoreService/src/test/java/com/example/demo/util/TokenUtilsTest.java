package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TokenUtils#extractTokenWithoutBearer(String)} 의 Bearer 접두사 제거 의미를 보장하는
 * 순수 단위 테스트 (Spring 컨텍스트/DB/주입 의존성 불필요).
 * extractTokenWithoutBearer 는 주입된 의존성을 사용하지 않으므로 {@code new TokenUtils(null, null)} 로 생성한다.
 */
class TokenUtilsTest {

    private final TokenUtils tokenUtils = new TokenUtils(null, null);

    @Test
    @DisplayName("선두 'Bearer ' 접두사는 제거된다")
    void stripsLeadingBearerPrefix() {
        assertThat(tokenUtils.extractTokenWithoutBearer("Bearer abc.def.ghi"))
                .isEqualTo("abc.def.ghi");
    }

    @Test
    @DisplayName("접두사가 없으면 토큰을 그대로 반환한다")
    void returnsTokenUnchangedWhenNoPrefix() {
        assertThat(tokenUtils.extractTokenWithoutBearer("abc.def.ghi"))
                .isEqualTo("abc.def.ghi");
    }

    @Test
    @DisplayName("null 은 null 을 반환한다")
    void returnsNullForNull() {
        assertThat(tokenUtils.extractTokenWithoutBearer(null)).isNull();
    }

    @Test
    @DisplayName("선두가 아닌 위치의 'Bearer ' 는 제거하지 않는다 (전역 치환이 아니라 접두사 전용)")
    void doesNotStripNonLeadingBearer() {
        assertThat(tokenUtils.extractTokenWithoutBearer("xBearer y"))
                .isEqualTo("xBearer y");
    }
}
