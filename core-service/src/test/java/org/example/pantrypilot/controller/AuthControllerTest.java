package org.example.pantrypilot.controller;

import java.time.Duration;

import jakarta.servlet.http.Cookie;

import org.hamcrest.Matchers;
import org.example.pantrypilot.config.AuthCookieProperties;
import org.example.pantrypilot.dto.ErrorResponse;
import org.example.pantrypilot.service.AuthService;
import org.example.pantrypilot.service.TokenPair;
import org.example.pantrypilot.service.exception.EmailAlreadyTakenException;
import org.example.pantrypilot.service.exception.InvalidCredentialsException;
import org.example.pantrypilot.service.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @Mock private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService, new AuthCookieProperties(false));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TestAuthAdvice())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void register_setsHttpOnlyRefreshCookieAndOmitsRefreshFromBody() throws Exception {
        when(authService.register(any())).thenReturn(
                new TokenPair("ACCESS", "REFRESH_RAW", 900L, Duration.ofDays(7)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("ACCESS"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString(REFRESH_COOKIE_NAME + "=REFRESH_RAW"),
                                Matchers.containsString("HttpOnly"),
                                Matchers.containsString("SameSite=Strict"),
                                Matchers.containsString("Path=/api/auth"),
                                Matchers.containsString("Max-Age=604800"))));
    }

    @Test
    void login_setsRefreshCookieAndReturnsAccessTokenBody() throws Exception {
        when(authService.login(any())).thenReturn(
                new TokenPair("ACCESS", "REFRESH_RAW", 900L, Duration.ofDays(7)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("ACCESS"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.containsString(REFRESH_COOKIE_NAME + "=REFRESH_RAW")));
    }

    @Test
    void refresh_withCookie_delegatesAndRotatesCookie() throws Exception {
        when(authService.refresh("REFRESH_RAW")).thenReturn(
                new TokenPair("NEW_ACCESS", "NEW_REFRESH_RAW", 900L, Duration.ofDays(7)));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, "REFRESH_RAW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("NEW_ACCESS"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.containsString(REFRESH_COOKIE_NAME + "=NEW_REFRESH_RAW")));

        verify(authService).refresh("REFRESH_RAW");
    }

    @Test
    void refresh_withoutCookie_returns401AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_refresh_token"));

        verifyNoInteractions(authService);
    }

    @Test
    void refresh_withBlankCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, "")))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).refresh(any());
    }

    @Test
    void logout_withCookie_revokesAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie(REFRESH_COOKIE_NAME, "REFRESH_RAW")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.allOf(
                                Matchers.containsString(REFRESH_COOKIE_NAME + "="),
                                Matchers.containsString("Max-Age=0"),
                                Matchers.containsString("Path=/api/auth"))));

        verify(authService).logout("REFRESH_RAW");
    }

    @Test
    void logout_withoutCookie_stillClearsCookieAndReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        Matchers.containsString("Max-Age=0")));

        verifyNoInteractions(authService);
    }

    @RestControllerAdvice
    static class TestAuthAdvice {

        @ExceptionHandler(InvalidRefreshTokenException.class)
        ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_refresh_token", ex.getMessage()));
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        ResponseEntity<ErrorResponse> handleInvalidCreds(InvalidCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("invalid_credentials", ex.getMessage()));
        }

        @ExceptionHandler(EmailAlreadyTakenException.class)
        ResponseEntity<ErrorResponse> handleEmailTaken(EmailAlreadyTakenException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("email_taken", ex.getMessage()));
        }
    }
}
