package com.example.demo.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {
    private final HttpCookieOAuth2AuthorizationRequestRepository repository = new HttpCookieOAuth2AuthorizationRequestRepository();

    @Test
    void storesStateAndRedirectOnlyInTheServerSessionAndRemovesStateOnce() {
        var request = new MockHttpServletRequest();
        request.setParameter("redirect_uri", "https://app.example.com/oauth2/redirect");
        var response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest(), request, response);

        assertThat(response.getCookies()).isEmpty();
        assertThat(repository.loadAuthorizationRequest(request).getState()).isEqualTo("state");
        assertThat(repository.getRedirectUri(request)).isEqualTo("https://app.example.com/oauth2/redirect");
        assertThat(repository.removeAuthorizationRequest(request, response).getState()).isEqualTo("state");
        assertThat(repository.loadAuthorizationRequest(request)).isNull();
        assertThat(repository.getRedirectUri(request)).isEqualTo("https://app.example.com/oauth2/redirect");
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
