package org.example.pantrypilot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator generator = new RefreshTokenGenerator();

    @Test
    void generate_producesUrlSafeTokensOfExpectedLength() {
        String token = generator.generate();

        assertThat(token)
                .isNotBlank()
                .matches("^[A-Za-z0-9_-]+$")
                .hasSizeBetween(42, 44);
    }

    @Test
    void generate_returnsDistinctTokensAcrossCalls() {
        String a = generator.generate();
        String b = generator.generate();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hash_isDeterministicAndSha256HexLength() {
        String hashA = generator.hash("input-token");
        String hashB = generator.hash("input-token");

        assertThat(hashA).isEqualTo(hashB).hasSize(64).matches("^[0-9a-f]+$");
    }

    @Test
    void hash_differsForDifferentInputs() {
        assertThat(generator.hash("token-one"))
                .isNotEqualTo(generator.hash("token-two"));
    }
}
