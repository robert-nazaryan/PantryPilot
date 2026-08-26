package org.example.pantrypilot.controller;

import java.time.Duration;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.config.AuthCookieProperties;
import org.example.pantrypilot.dto.AuthResponse;
import org.example.pantrypilot.dto.LoginRequest;
import org.example.pantrypilot.dto.RegisterRequest;
import org.example.pantrypilot.service.AuthService;
import org.example.pantrypilot.service.TokenPair;
import org.example.pantrypilot.service.exception.InvalidRefreshTokenException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE = "Strict";

    private final AuthService authService;
    private final AuthCookieProperties cookieProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return respondWithTokens(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return respondWithTokens(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        requireRefreshCookie(refreshToken);
        return respondWithTokens(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respondWithTokens(HttpStatus status, TokenPair pair) {
        AuthResponse body = new AuthResponse(pair.accessToken(), pair.accessTokenTtlSeconds());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(pair.rawRefreshToken(), pair.refreshTokenTtl()).toString())
                .body(body);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private static void requireRefreshCookie(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
    }
}
