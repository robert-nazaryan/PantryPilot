package org.example.pantrypilot.service.ai;

import java.util.function.Supplier;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Retry helper for AI provider calls. Retries with exponential backoff on transient
 * failure classes that both Gemini and Groq exhibit under load:
 * <ul>
 *   <li>HTTP 429 Too Many Requests (short-lived rate limit)</li>
 *   <li>HTTP 502 Bad Gateway (upstream restart)</li>
 *   <li>HTTP 503 Service Unavailable (documented transient state on both providers)</li>
 *   <li>HTTP 504 Gateway Timeout (upstream took too long)</li>
 *   <li>{@link ResourceAccessException} (I/O errors: connection reset, socket timeout,
 *       DNS blip — anything the RestClient couldn't complete a round-trip on)</li>
 * </ul>
 * Everything else — 4xx auth/validation, 5xx that is not one of the above, non-HTTP
 * errors — is re-thrown immediately so we don't retry a request that has no chance
 * of succeeding.
 */
final class TransientAiRetry {

    static final int DEFAULT_MAX_ATTEMPTS = 4;
    static final long DEFAULT_INITIAL_BACKOFF_MS = 500L;

    private TransientAiRetry() {
    }

    static <T> T call(Supplier<T> attempt) {
        return call(attempt, DEFAULT_MAX_ATTEMPTS, DEFAULT_INITIAL_BACKOFF_MS, defaultSleeper());
    }

    static <T> T call(Supplier<T> attempt, int maxAttempts, long initialBackoffMs, Sleeper sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        RestClientException last = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return attempt.get();
            } catch (HttpStatusCodeException ex) {
                if (!isRetryable(ex.getStatusCode())) {
                    throw ex;
                }
                last = ex;
            } catch (ResourceAccessException ex) {
                last = ex;
            }
            if (i + 1 == maxAttempts) {
                break;
            }
            long backoff = initialBackoffMs << i;
            try {
                sleeper.pause(backoff);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while backing off from AI provider", ie);
            }
        }
        throw last;
    }

    private static boolean isRetryable(HttpStatusCode status) {
        int code = status.value();
        return code == 429 || code == 502 || code == 503 || code == 504;
    }

    static Sleeper defaultSleeper() {
        return Thread::sleep;
    }

    @FunctionalInterface
    interface Sleeper {
        void pause(long ms) throws InterruptedException;
    }
}
