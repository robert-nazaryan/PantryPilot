package org.example.pantrypilot.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransientGeminiRetryTest {

    @Test
    void call_returnsResultOnFirstAttemptWithoutSleeping() {
        List<Long> sleeps = new ArrayList<>();
        String out = TransientGeminiRetry.call(() -> "ok", 3, 100L, sleeps::add);
        assertThat(out).isEqualTo("ok");
        assertThat(sleeps).isEmpty();
    }

    @Test
    void call_retriesWithExponentialBackoffAndSucceedsOnLaterAttempt() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> flaky = () -> {
            int n = attempts.incrementAndGet();
            if (n < 3) {
                throw serviceUnavailable();
            }
            return "ok on attempt " + n;
        };
        List<Long> sleeps = new ArrayList<>();

        String out = TransientGeminiRetry.call(flaky, 4, 100L, sleeps::add);

        assertThat(out).isEqualTo("ok on attempt 3");
        assertThat(sleeps).containsExactly(100L, 200L);
    }

    @Test
    void call_afterAllAttemptsFail_throwsLast503WithoutFinalSleep() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> alwaysDown = () -> {
            attempts.incrementAndGet();
            throw serviceUnavailable();
        };
        List<Long> sleeps = new ArrayList<>();

        assertThatThrownBy(() -> TransientGeminiRetry.call(alwaysDown, 3, 100L, sleeps::add))
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);

        assertThat(attempts.get()).isEqualTo(3);
        assertThat(sleeps).containsExactly(100L, 200L);
    }

    @Test
    void call_nonServiceUnavailableExceptions_areNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> nonRetryable = () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("boom");
        };
        List<Long> sleeps = new ArrayList<>();

        assertThatThrownBy(() -> TransientGeminiRetry.call(nonRetryable, 3, 100L, sleeps::add))
                .isInstanceOf(IllegalStateException.class);
        assertThat(attempts.get()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void call_zeroMaxAttempts_throwsIllegalArgument() {
        assertThatThrownBy(() -> TransientGeminiRetry.call(() -> "ok", 0, 100L, ms -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void call_interruptedSleep_setsInterruptFlagAndThrows() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> down = () -> {
            attempts.incrementAndGet();
            throw serviceUnavailable();
        };
        TransientGeminiRetry.Sleeper interruptor = ms -> {
            throw new InterruptedException("test");
        };

        assertThatThrownBy(() -> TransientGeminiRetry.call(down, 3, 100L, interruptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Interrupted");
        assertThat(Thread.interrupted()).isTrue();
    }

    private static HttpServerErrorException.ServiceUnavailable serviceUnavailable() {
        return (HttpServerErrorException.ServiceUnavailable) HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                HttpHeaders.EMPTY, new byte[0], null);
    }
}
