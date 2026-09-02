package org.example.pantrypilot.service.ai;

import java.util.function.Supplier;

import org.springframework.web.client.HttpServerErrorException;

final class TransientGeminiRetry {

    static final int DEFAULT_MAX_ATTEMPTS = 4;
    static final long DEFAULT_INITIAL_BACKOFF_MS = 500L;

    private TransientGeminiRetry() {
    }

    static <T> T call(Supplier<T> attempt) {
        return call(attempt, DEFAULT_MAX_ATTEMPTS, DEFAULT_INITIAL_BACKOFF_MS, defaultSleeper());
    }

    static <T> T call(Supplier<T> attempt, int maxAttempts, long initialBackoffMs, Sleeper sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        HttpServerErrorException.ServiceUnavailable last = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return attempt.get();
            } catch (HttpServerErrorException.ServiceUnavailable ex) {
                last = ex;
                if (i + 1 == maxAttempts) {
                    break;
                }
                long backoff = initialBackoffMs << i;
                try {
                    sleeper.pause(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while backing off from Gemini 503", ie);
                }
            }
        }
        throw last;
    }

    static Sleeper defaultSleeper() {
        return Thread::sleep;
    }

    @FunctionalInterface
    interface Sleeper {
        void pause(long ms) throws InterruptedException;
    }
}
