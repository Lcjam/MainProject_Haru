package com.example.demo.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AuthenticationFailureHandlerTest {
    private final HttpCookieOAuth2AuthorizationRequestRepository repository = new HttpCookieOAuth2AuthorizationRequestRepository();
    private final OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler(
            repository, List.of("https://app.example.com:443/oauth2/redirect"));

    @Test
    void rejectsForeignAndMalformedRedirectCookieWithoutLeakingException() throws Exception {
        for (String redirect : List.of("http://app.example.com:443/redirect",
                "https://evil.example.com:443/redirect", "https://app.example.com:443/%zz")) {
            var request = new MockHttpServletRequest();
            request.setParameter("redirect_uri", redirect);
            repository.saveAuthorizationRequest(authorizationRequest(), request, new MockHttpServletResponse());
            var response = new MockHttpServletResponse();

            handler.onAuthenticationFailure(request, response, new BadCredentialsException("raw-secret-token"));

            assertThat(response.getRedirectedUrl()).isEqualTo("/?error=authentication_failed");
            assertThat(response.getRedirectedUrl()).doesNotContain("raw-secret-token");
        }
    }

    @Test
    void keepsAuthorizedOriginForFailureRedirect() throws Exception {
        var request = new MockHttpServletRequest();
        request.setParameter("redirect_uri", "https://app.example.com:443/login");
        repository.saveAuthorizationRequest(authorizationRequest(), request, new MockHttpServletResponse());
        var response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("raw-secret-token"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://app.example.com:443/login?error=authentication_failed");
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
