package com.example.demo.security.oauth2;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final List<String> authorizedRedirectUris;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되었습니다. 리다이렉트할 수 없습니다: " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new IllegalArgumentException("승인되지 않은 리다이렉션 URI가 있어 인증을 진행할 수 없습니다");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 권한 리스트 가져오기
        List<String> roles = oAuth2User.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(oAuth2User.getUserId(), oAuth2User.getEmail(), roles);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .queryParam("email", oAuth2User.getEmail())
                .build().toUriString();
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);
        return authorizedRedirectUris.stream()
                .map(URI::create)
                .anyMatch(authorizedUri -> 
                        authorizedUri.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                        && authorizedUri.getPort() == clientRedirectUri.getPort());
    }
    
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        CookieUtils.deleteCookie(request, response, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
