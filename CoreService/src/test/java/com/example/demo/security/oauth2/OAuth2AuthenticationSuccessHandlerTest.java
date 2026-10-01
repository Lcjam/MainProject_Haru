package com.example.demo.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2AuthenticationSuccessHandlerTest {
    private final HttpCookieOAuth2AuthorizationRequestRepository repository = new HttpCookieOAuth2AuthorizationRequestRepository();
    private final OAuth2LoginCodeService codeService = mock(OAuth2LoginCodeService.class);

    private final OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
            List.of("https://app.example.com:443/oauth2/redirect"),
            repository, codeService);

    @Test
    void acceptsSameSchemeHostAndPortRegardlessOfPath() {
        assertThat(handler.isAuthorizedRedirectUri("https://APP.example.com:443/another/path?next=1"))
                .isTrue();
    }

    @Test
    void rejectsDifferentOriginComponentsAndMalformedUris() {
        assertThat(handler.isAuthorizedRedirectUri("http://app.example.com:443/oauth2/redirect")).isFalse();
        assertThat(handler.isAuthorizedRedirectUri("https://app.example.com:444/oauth2/redirect")).isFalse();
        assertThat(handler.isAuthorizedRedirectUri("https://evil.example.com:443/oauth2/redirect")).isFalse();
        assertThat(handler.isAuthorizedRedirectUri("/oauth2/redirect")).isFalse();
        assertThat(handler.isAuthorizedRedirectUri("https://app.example.com:443/%zz")).isFalse();
        assertThat(handler.isAuthorizedRedirectUri(null)).isFalse();
    }

    @Test
    void redirectsOnlyWithAnExchangeCode() {
        var request = new MockHttpServletRequest();
        request.setParameter("redirect_uri", "https://app.example.com:443/oauth2/redirect");
        repository.saveAuthorizationRequest(authorizationRequest(), request, new MockHttpServletResponse());
        CustomOAuth2User user = new CustomOAuth2User(7, "user@example.com", "Haru",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("email", "user@example.com"), "email");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(codeService.issue(any(), any())).thenReturn("short-lived-code");

        String target = handler.determineTargetUrl(request, new MockHttpServletResponse(), authentication);

        assertThat(target).isEqualTo("https://app.example.com:443/oauth2/redirect?code=short-lived-code");
        assertThat(target).doesNotContain("token", "user@example.com");
    }

    private static OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://provider.example.com/authorize")
                .clientId("client")
                .redirectUri("http://localhost/callback")
                .state("state")
                .build();
    }
}
