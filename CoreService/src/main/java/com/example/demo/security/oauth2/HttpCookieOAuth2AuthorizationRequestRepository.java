package com.example.demo.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String AUTHORIZATION_REQUEST_ATTRIBUTE =
            HttpCookieOAuth2AuthorizationRequestRepository.class.getName() + ".authorizationRequest";
    private static final String REDIRECT_URI_ATTRIBUTE =
            HttpCookieOAuth2AuthorizationRequestRepository.class.getName() + ".redirectUri";
    private static final String REDIRECT_URI_PARAMETER = "redirect_uri";
    private static final Duration REQUEST_TTL = Duration.ofMinutes(3);

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        synchronized (session) {
            StoredAuthorizationRequest stored = (StoredAuthorizationRequest) session.getAttribute(AUTHORIZATION_REQUEST_ATTRIBUTE);
            if (stored == null || isExpired(stored.createdAt())) {
                removeAuthorizationRequest(session);
                return null;
            }
            return stored.request();
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                synchronized (session) {
                    removeAuthorizationRequest(session);
                    clearRedirectUri(session);
                }
            }
            return;
        }

        HttpSession session = request.getSession();
        Instant createdAt = Instant.now();
        session.setAttribute(AUTHORIZATION_REQUEST_ATTRIBUTE, new StoredAuthorizationRequest(authorizationRequest, createdAt));
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAMETER);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            session.setAttribute(REDIRECT_URI_ATTRIBUTE, new StoredRedirectUri(redirectUriAfterLogin, createdAt));
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        synchronized (session) {
            StoredAuthorizationRequest stored = (StoredAuthorizationRequest) session.getAttribute(AUTHORIZATION_REQUEST_ATTRIBUTE);
            removeAuthorizationRequest(session);
            if (stored == null || isExpired(stored.createdAt())) {
                clearRedirectUri(session);
                return null;
            }
            return stored.request();
        }
    }

    public String getRedirectUri(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        synchronized (session) {
            StoredRedirectUri stored = (StoredRedirectUri) session.getAttribute(REDIRECT_URI_ATTRIBUTE);
            if (stored == null || isExpired(stored.createdAt())) {
                clearRedirectUri(session);
                return null;
            }
            return stored.uri();
        }
    }

    public void clearRedirectUri(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) {
                clearRedirectUri(session);
            }
        }
    }

    private boolean isExpired(Instant createdAt) {
        return createdAt.plus(REQUEST_TTL).isBefore(Instant.now());
    }

    private void removeAuthorizationRequest(HttpSession session) {
        session.removeAttribute(AUTHORIZATION_REQUEST_ATTRIBUTE);
    }

    private void clearRedirectUri(HttpSession session) {
        session.removeAttribute(REDIRECT_URI_ATTRIBUTE);
    }

    private record StoredAuthorizationRequest(OAuth2AuthorizationRequest request, Instant createdAt) { }

    private record StoredRedirectUri(String uri, Instant createdAt) { }
}
