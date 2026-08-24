package org.example.pantrypilot.service;

import java.time.Duration;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.example.pantrypilot.config.JwtProperties;
import org.example.pantrypilot.dto.AuthResponse;
import org.example.pantrypilot.dto.LoginRequest;
import org.example.pantrypilot.dto.RegisterRequest;
import org.example.pantrypilot.model.RefreshToken;
import org.example.pantrypilot.model.User;
import org.example.pantrypilot.repository.RefreshTokenRepository;
import org.example.pantrypilot.repository.UserRepository;
import org.example.pantrypilot.service.exception.EmailAlreadyTakenException;
import org.example.pantrypilot.service.exception.InvalidCredentialsException;
import org.example.pantrypilot.service.exception.InvalidRefreshTokenException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyTakenException();
        }
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .build();
        return issueTokenPair(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = lookupActiveRefreshToken(rawRefreshToken);
        stored.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(stored);
        return issueTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .filter(rt -> rt.getRevokedAt() == null)
                .ifPresent(rt -> {
                    rt.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(rt);
                });
    }

    private RefreshToken lookupActiveRefreshToken(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (stored.getRevokedAt() != null
                || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }
        return stored;
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtService.issueAccessToken(user);
        String rawRefresh = refreshTokenGenerator.generate();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenGenerator.hash(rawRefresh))
                .expiresAt(OffsetDateTime.now().plus(refreshTokenTtl()))
                .build();
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, rawRefresh, jwtService.getAccessTokenTtlSeconds());
    }

    private Duration refreshTokenTtl() {
        return Duration.ofDays(jwtProperties.refreshTokenTtlDays());
    }
}
