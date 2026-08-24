package org.example.pantrypilot.service;

import io.jsonwebtoken.security.SignatureException;
import org.example.pantrypilot.config.JwtProperties;
import org.example.pantrypilot.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-min-32-bytes-length-padding-xyz";
    private final JwtService service = new JwtService(new JwtProperties(SECRET, 15L, 7L));

    @Test
    void issueAccessToken_thenParseUserId_roundTripsTheSubject() {
        User user = User.builder().id(42L).email("bob@example.com").build();

        String token = service.issueAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(service.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void parseUserId_withWrongSignature_throws() {
        User user = User.builder().id(1L).email("a@b.com").build();
        JwtService differentKey = new JwtService(new JwtProperties(
                "totally-different-secret-of-at-least-32-bytes-abc", 15L, 7L));
        String tokenSignedByOther = differentKey.issueAccessToken(user);

        assertThatThrownBy(() -> service.parseUserId(tokenSignedByOther))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void getAccessTokenTtlSeconds_matchesConfiguredMinutes() {
        assertThat(service.getAccessTokenTtlSeconds()).isEqualTo(15L * 60L);
    }
}
