package com.example.demo.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final List<String> authorizedRedirectUris;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final OAuth2LoginCodeService loginCodeService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되어 OAuth2 리다이렉트를 건너뜁니다");
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String redirectUri = authorizationRequestRepository.getRedirectUri(request);

        if (redirectUri != null && !isAuthorizedRedirectUri(redirectUri)) {
            throw new IllegalArgumentException("승인되지 않은 리다이렉션 URI가 있어 인증을 진행할 수 없습니다");
        }

        String targetUrl = redirectUri != null ? redirectUri : getDefaultTargetUrl();

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String code = loginCodeService.issue(request, oAuth2User);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .replaceQueryParam("code", code)
                .build().toUriString();
    }

    boolean isAuthorizedRedirectUri(String uri) {
        return isAuthorizedRedirectUri(uri, authorizedRedirectUris);
    }

    static boolean isAuthorizedRedirectUri(String uri, List<String> authorizedRedirectUris) {
        try {
            URI clientRedirectUri = URI.create(uri);
            if (clientRedirectUri.getScheme() == null || clientRedirectUri.getHost() == null) {
                return false;
            }
            return authorizedRedirectUris.stream()
                    .map(URI::create)
                    .anyMatch(authorizedUri ->
                            authorizedUri.getScheme().equalsIgnoreCase(clientRedirectUri.getScheme())
                            && authorizedUri.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                            && authorizedUri.getPort() == clientRedirectUri.getPort());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }
    
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        authorizationRequestRepository.clearRedirectUri(request);
    }
}
