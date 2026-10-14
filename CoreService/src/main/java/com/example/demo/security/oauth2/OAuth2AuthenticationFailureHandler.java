package com.example.demo.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final List<String> authorizedRedirectUris;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String targetUrl = java.util.Optional.ofNullable(authorizationRequestRepository.getRedirectUri(request))
                .filter(uri -> OAuth2AuthenticationSuccessHandler.isAuthorizedRedirectUri(uri, authorizedRedirectUris))
                .orElse("/");
        
        log.warn("OAuth2 인증 실패: {}", exception.getClass().getSimpleName());
        
        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", "authentication_failed")
                .build().toUriString();
                
        // 인증 관련 속성 및 쿠키 제거
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        authorizationRequestRepository.clearRedirectUri(request);
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
