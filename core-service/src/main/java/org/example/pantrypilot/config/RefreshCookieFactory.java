package org.example.pantrypilot.config;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    public static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE = "Strict";

    private final AuthCookieProperties cookieProperties;

    public ResponseCookie build(String value, Duration maxAge) {
        return baseCookie(value).maxAge(maxAge).build();
    }

    public ResponseCookie expired() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH);
    }
}
