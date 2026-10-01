package com.example.demo.security.oauth2;

import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2LoginCodeServiceTest {
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final OAuth2LoginCodeService service = new OAuth2LoginCodeService(tokenProvider);

    @Test
    void exchangesCodeOnceOnlyForTheIssuingBrowserSession() {
        when(tokenProvider.createToken(any(), any(), any())).thenReturn("jwt-value");
        var session = new MockHttpSession();
        var issuingRequest = new MockHttpServletRequest();
        issuingRequest.setSession(session);
        String code = service.issue(issuingRequest, user());

        var sameBrowser = new MockHttpServletRequest();
        sameBrowser.setSession(session);
        assertThat(service.consume(sameBrowser, code)).extracting("token", "email", "nickname")
                .containsExactly("jwt-value", "user@example.com", "Haru");
        assertThat(service.consume(sameBrowser, code)).isNull();

        var otherBrowser = new MockHttpServletRequest();
        otherBrowser.setSession(new MockHttpSession());
        assertThat(service.consume(otherBrowser, code)).isNull();
    }

    private static CustomOAuth2User user() {
        return new CustomOAuth2User(7, "user@example.com", "Haru",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("email", "user@example.com"), "email");
    }
}
