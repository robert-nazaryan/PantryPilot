package org.example.pantrypilot.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.config.RefreshCookieFactory;
import org.example.pantrypilot.dto.AuthResponse;
import org.example.pantrypilot.dto.LoginRequest;
import org.example.pantrypilot.dto.RegisterRequest;
import org.example.pantrypilot.service.AuthService;
import org.example.pantrypilot.service.TokenPair;
import org.example.pantrypilot.service.exception.InvalidRefreshTokenException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

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
            @CookieValue(name = RefreshCookieFactory.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        requireRefreshCookie(refreshToken);
        return respondWithTokens(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.expired().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respondWithTokens(HttpStatus status, TokenPair pair) {
        AuthResponse body = new AuthResponse(pair.accessToken(), pair.accessTokenTtlSeconds());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookieFactory.build(pair.rawRefreshToken(), pair.refreshTokenTtl()).toString())
                .body(body);
    }

    private static void requireRefreshCookie(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
    }
}
