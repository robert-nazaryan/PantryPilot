package org.example.pantrypilot.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.service.AuthService;
import org.example.pantrypilot.service.AuthenticatedSession;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String subject = principal.getAttribute("sub");
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        Boolean emailVerified = principal.getAttribute("email_verified");

        if (subject == null || email == null
                || (emailVerified != null && !emailVerified)) {
            redirectToLoginWithError(response, "google_login_failed");
            return;
        }

        AuthenticatedSession session = authService.loginWithGoogle(subject, email, name);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookieFactory.build(session.pair().rawRefreshToken(), session.pair().refreshTokenTtl()).toString());

        StringBuilder target = new StringBuilder(appProperties.url())
                .append("/auth/callback")
                .append("?accessToken=").append(urlEncode(session.pair().accessToken()))
                .append("&expiresIn=").append(session.pair().accessTokenTtlSeconds());
        if (session.displayName() != null && !session.displayName().isBlank()) {
            target.append("&displayName=").append(urlEncode(session.displayName()));
        }
        response.sendRedirect(target.toString());
    }

    private void redirectToLoginWithError(HttpServletResponse response, String errorCode) throws IOException {
        response.sendRedirect(appProperties.url() + "/login?error=" + urlEncode(errorCode));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
