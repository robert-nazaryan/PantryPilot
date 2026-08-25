package org.example.pantrypilot.security;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserIdArgumentResolverTest {

    private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsParameter_annotatedLong_returnsTrue() throws Exception {
        assertThat(resolver.supportsParameter(paramFor("annotatedLong"))).isTrue();
    }

    @Test
    void supportsParameter_unannotatedLong_returnsFalse() throws Exception {
        assertThat(resolver.supportsParameter(paramFor("plainLong"))).isFalse();
    }

    @Test
    void supportsParameter_annotatedString_returnsFalse() throws Exception {
        assertThat(resolver.supportsParameter(paramFor("annotatedString"))).isFalse();
    }

    @Test
    void resolveArgument_authenticatedUserPrincipal_returnsId() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(42L), null, of()));

        Long id = resolver.resolveArgument(paramFor("annotatedLong"), null, null, null);

        assertThat(id).isEqualTo(42L);
    }

    @Test
    void resolveArgument_nullAuthentication_throwsIllegalState() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> resolver.resolveArgument(paramFor("annotatedLong"), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    void resolveArgument_wrongPrincipalType_throwsIllegalState() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("some-string-principal", null, of()));

        assertThatThrownBy(() -> resolver.resolveArgument(paramFor("annotatedLong"), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected principal type");
    }

    @Test
    void resolveArgument_anonymousAuthentication_throwsIllegalState() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anon", of(new SimpleGrantedAuthority("ROLE_ANON"))));

        assertThatThrownBy(() -> resolver.resolveArgument(paramFor("annotatedLong"), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected principal type");
    }

    private MethodParameter paramFor(String methodName) throws NoSuchMethodException {
        Method method = Fixtures.class.getDeclaredMethod(methodName, methodName.contains("String")
                ? String.class : Long.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    static class Fixtures {
        void annotatedLong(@CurrentUserId Long userId) {}

        void plainLong(Long userId) {}

        void annotatedString(@CurrentUserId String userId) {}
    }
}
