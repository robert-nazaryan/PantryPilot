package org.example.pantrypilot.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.example.pantrypilot.config.JwtProperties;
import org.example.pantrypilot.dto.LoginRequest;
import org.example.pantrypilot.dto.RegisterRequest;
import org.example.pantrypilot.model.RefreshToken;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.RefreshTokenRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.EmailAlreadyTakenException;
import org.example.pantrypilot.service.exception.InvalidCredentialsException;
import org.example.pantrypilot.service.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_REFRESH = "raw-refresh-token";
    private static final String HASHED_REFRESH = "hashed-refresh-token";

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenGenerator refreshTokenGenerator;

    private final JwtProperties jwtProperties = new JwtProperties("secret", 15L, 7L);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtService, refreshTokenGenerator, jwtProperties);
    }

    @Test
    void register_success_savesUserAndIssuesTokenPair() {
        final RegisterRequest req = new RegisterRequest("alice@example.com", "password123", "Alice");
        User saved = userWithId(1L, "alice@example.com", "HASHED");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        stubTokenIssuance(saved);

        TokenPair resp = authService.register(req);

        assertTokenPairMatches(resp);
        verify(refreshTokenRepository).save(argThat(rt -> rt.getTokenHash().equals(HASHED_REFRESH)
                && rt.getUser() == saved
                && rt.getExpiresAt().isAfter(OffsetDateTime.now())));
    }

    @Test
    void register_duplicateEmail_throwsAndSavesNothing() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice@example.com", "password123", null)))
                .isInstanceOf(EmailAlreadyTakenException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenGenerator);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_success_issuesTokenPair() {
        User user = userWithId(1L, "alice@example.com", "HASHED");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASHED")).thenReturn(true);
        stubTokenIssuance(user);

        TokenPair resp = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertTokenPairMatches(resp);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = userWithId(1L, "alice@example.com", "HASHED");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenGenerator);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "x")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenGenerator);
    }

    @Test
    void login_googleOnlyAccount_throwsInvalidCredentialsWithoutLeaking() {
        User googleOnly = User.builder()
                .id(1L)
                .email("google@example.com")
                .passwordHash(null)
                .googleId("google-sub-123")
                .build();
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(googleOnly));

        assertThatThrownBy(() -> authService.login(new LoginRequest("google@example.com", "any")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenGenerator);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginWithGoogle_existingGoogleUser_issuesTokensNoLinkageWrite() {
        User user = googleUserWithDisplayName(1L, "chef@example.com", "google-sub-1", "Chef Robert");
        when(userRepository.findByGoogleId("google-sub-1")).thenReturn(Optional.of(user));
        stubTokenIssuance(user);

        AuthenticatedSession session = authService.loginWithGoogle("google-sub-1", "chef@example.com", "Chef");

        assertTokenPairMatches(session.pair());
        assertThat(session.displayName()).isEqualTo("Chef Robert");
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loginWithGoogle_existingEmailPasswordUser_linksGoogleIdAndIssuesTokens() {
        User existing = userWithId(1L, "shared@example.com", "HASHED");
        existing.setDisplayName("Shared Existing");
        when(userRepository.findByGoogleId("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("shared@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        stubTokenIssuance(existing);

        AuthenticatedSession session = authService.loginWithGoogle("google-sub-2", "shared@example.com", "Shared");

        assertTokenPairMatches(session.pair());
        assertThat(session.displayName()).isEqualTo("Shared Existing");
        assertThat(existing.getGoogleId()).isEqualTo("google-sub-2");
        assertThat(existing.getPasswordHash()).isEqualTo("HASHED");
        verify(userRepository).save(existing);
    }

    @Test
    void loginWithGoogle_unknownAccount_createsNewUserWithoutPassword() {
        when(userRepository.findByGoogleId("google-sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.issueAccessToken(any(User.class))).thenReturn("ACCESS_TOKEN");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenGenerator.generate()).thenReturn(RAW_REFRESH);
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);

        AuthenticatedSession session = authService.loginWithGoogle("google-sub-3", "newbie@example.com", "Newbie");

        assertThat(session.pair().accessToken()).isEqualTo("ACCESS_TOKEN");
        assertThat(session.displayName()).isEqualTo("Newbie");
        verify(userRepository).save(argThat(u ->
                "newbie@example.com".equals(u.getEmail())
                        && "google-sub-3".equals(u.getGoogleId())
                        && "Newbie".equals(u.getDisplayName())
                        && u.getPasswordHash() == null));
    }

    @Test
    void refresh_success_rotatesToken() {
        User user = userWithId(1L, "alice@example.com", "HASHED");
        RefreshToken existing = activeRefreshToken(user);
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.of(existing));
        stubTokenIssuance(user);
        String newRaw = "rotated-raw";
        String newHash = "rotated-hash";
        when(refreshTokenGenerator.generate()).thenReturn(newRaw);
        when(refreshTokenGenerator.hash(newRaw)).thenReturn(newHash);

        TokenPair resp = authService.refresh(RAW_REFRESH);

        assertThat(resp.rawRefreshToken()).isEqualTo(newRaw);
        assertThat(resp.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(existing.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(existing);
        verify(refreshTokenRepository).save(argThat(rt -> newHash.equals(rt.getTokenHash())));
    }

    @Test
    void refresh_unknownToken_throwsInvalidRefreshToken() {
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_expiredToken_throwsInvalidRefreshToken() {
        RefreshToken expired = RefreshToken.builder()
                .user(userWithId(1L, "a@b.com", "H"))
                .tokenHash(HASHED_REFRESH)
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(jwtService);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_alreadyRevokedToken_throwsInvalidRefreshToken() {
        RefreshToken revoked = activeRefreshToken(userWithId(1L, "a@b.com", "H"));
        revoked.setRevokedAt(OffsetDateTime.now().minusMinutes(1));
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(RAW_REFRESH))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(jwtService);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_activeToken_marksRevoked() {
        RefreshToken active = activeRefreshToken(userWithId(1L, "a@b.com", "H"));
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.of(active));

        authService.logout(RAW_REFRESH);

        assertThat(active.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(active);
    }

    @Test
    void logout_alreadyRevokedToken_isNoOp() {
        RefreshToken active = activeRefreshToken(userWithId(1L, "a@b.com", "H"));
        OffsetDateTime original = OffsetDateTime.now().minusHours(1);
        active.setRevokedAt(original);
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.of(active));

        authService.logout(RAW_REFRESH);

        assertThat(active.getRevokedAt()).isEqualTo(original);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_unknownToken_isNoOp() {
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
        when(refreshTokenRepository.findByTokenHash(HASHED_REFRESH)).thenReturn(Optional.empty());

        authService.logout(RAW_REFRESH);

        verify(refreshTokenRepository, never()).save(any());
    }

    private void stubTokenIssuance(User user) {
        when(jwtService.issueAccessToken(user)).thenReturn("ACCESS_TOKEN");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenGenerator.generate()).thenReturn(RAW_REFRESH);
        when(refreshTokenGenerator.hash(RAW_REFRESH)).thenReturn(HASHED_REFRESH);
    }

    private void assertTokenPairMatches(TokenPair pair) {
        assertThat(pair.accessToken()).isEqualTo("ACCESS_TOKEN");
        assertThat(pair.rawRefreshToken()).isEqualTo(RAW_REFRESH);
        assertThat(pair.accessTokenTtlSeconds()).isEqualTo(900L);
        assertThat(pair.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
    }

    private static User userWithId(Long id, String email, String hash) {
        return User.builder().id(id).email(email).passwordHash(hash).build();
    }

    private static User googleUserWithDisplayName(Long id, String email, String googleId, String displayName) {
        return User.builder().id(id).email(email).googleId(googleId).displayName(displayName).build();
    }

    private static RefreshToken activeRefreshToken(User user) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash(HASHED_REFRESH)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
    }
}
