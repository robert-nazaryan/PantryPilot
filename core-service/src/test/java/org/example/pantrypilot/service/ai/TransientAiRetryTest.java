package org.example.pantrypilot.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransientAiRetryTest {

    @Test
    void call_returnsResultOnFirstAttemptWithoutSleeping() {
        List<Long> sleeps = new ArrayList<>();
        String out = TransientAiRetry.call(() -> "ok", 3, 100L, sleeps::add);
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

        String out = TransientAiRetry.call(flaky, 4, 100L, sleeps::add);

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

        assertThatThrownBy(() -> TransientAiRetry.call(alwaysDown, 3, 100L, sleeps::add))
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

        assertThatThrownBy(() -> TransientAiRetry.call(nonRetryable, 3, 100L, sleeps::add))
                .isInstanceOf(IllegalStateException.class);
        assertThat(attempts.get()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void call_zeroMaxAttempts_throwsIllegalArgument() {
        assertThatThrownBy(() -> TransientAiRetry.call(() -> "ok", 0, 100L, ms -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void call_interruptedSleep_setsInterruptFlagAndThrows() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> down = () -> {
            attempts.incrementAndGet();
            throw serviceUnavailable();
        };
        TransientAiRetry.Sleeper interruptor = ms -> {
            throw new InterruptedException("test");
        };

        assertThatThrownBy(() -> TransientAiRetry.call(down, 3, 100L, interruptor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Interrupted");
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void call_retriesRateLimit429() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> flaky = () -> {
            int n = attempts.incrementAndGet();
            if (n < 3) {
                throw HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "429",
                        HttpHeaders.EMPTY, new byte[0], null);
            }
            return "ok";
        };
        List<Long> sleeps = new ArrayList<>();

        String out = TransientAiRetry.call(flaky, 4, 100L, sleeps::add);

        assertThat(out).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(sleeps).containsExactly(100L, 200L);
    }

    @Test
    void call_retriesGatewayTimeout504() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> flaky = () -> {
            int n = attempts.incrementAndGet();
            if (n < 2) {
                throw HttpServerErrorException.create(
                        HttpStatus.GATEWAY_TIMEOUT, "504",
                        HttpHeaders.EMPTY, new byte[0], null);
            }
            return "ok";
        };
        List<Long> sleeps = new ArrayList<>();

        String out = TransientAiRetry.call(flaky, 4, 100L, sleeps::add);

        assertThat(out).isEqualTo("ok");
        assertThat(sleeps).containsExactly(100L);
    }

    @Test
    void call_retriesBadGateway502() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> flaky = () -> {
            int n = attempts.incrementAndGet();
            if (n < 2) {
                throw HttpServerErrorException.create(
                        HttpStatus.BAD_GATEWAY, "502",
                        HttpHeaders.EMPTY, new byte[0], null);
            }
            return "ok";
        };
        List<Long> sleeps = new ArrayList<>();

        String out = TransientAiRetry.call(flaky, 4, 100L, sleeps::add);

        assertThat(out).isEqualTo("ok");
        assertThat(sleeps).containsExactly(100L);
    }

    @Test
    void call_retriesResourceAccessException() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> flaky = () -> {
            int n = attempts.incrementAndGet();
            if (n < 3) {
                throw new ResourceAccessException("connection reset",
                        new SocketTimeoutException("read timeout"));
            }
            return "ok";
        };
        List<Long> sleeps = new ArrayList<>();

        String out = TransientAiRetry.call(flaky, 4, 100L, sleeps::add);

        assertThat(out).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(sleeps).containsExactly(100L, 200L);
    }

    @Test
    void call_doesNotRetryNonRetryable4xx() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> unauthorized = () -> {
            attempts.incrementAndGet();
            throw HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED, "401",
                    HttpHeaders.EMPTY, new byte[0], null);
        };
        List<Long> sleeps = new ArrayList<>();

        assertThatThrownBy(() -> TransientAiRetry.call(unauthorized, 3, 100L, sleeps::add))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        assertThat(attempts.get()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void call_doesNotRetryNon429Non502Non503Non5045xx() {
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> internalError = () -> {
            attempts.incrementAndGet();
            throw HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "500",
                    HttpHeaders.EMPTY, new byte[0], null);
        };
        List<Long> sleeps = new ArrayList<>();

        assertThatThrownBy(() -> TransientAiRetry.call(internalError, 3, 100L, sleeps::add))
                .isInstanceOf(HttpServerErrorException.InternalServerError.class);
        assertThat(attempts.get()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    private static HttpServerErrorException.ServiceUnavailable serviceUnavailable() {
        return (HttpServerErrorException.ServiceUnavailable) HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                HttpHeaders.EMPTY, new byte[0], null);
    }
}
