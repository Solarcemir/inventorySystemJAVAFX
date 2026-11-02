package com.inventory.backend.services;

import com.inventory.backend.exception.ApiException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Makes a repeated request run only once. Keys live in memory for a few minutes,
 * which covers double clicks and retries; there is a single server and no table for them.
 */
@Service
public class IdempotencyService {

    private record Attempt(CompletableFuture<Object> result, Instant startedAt) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Duration keyLifetime;

    public IdempotencyService() {
        this(Duration.ofMinutes(10));
    }

    public IdempotencyService(Duration keyLifetime) {
        this.keyLifetime = keyLifetime;
    }

    /**
     * Runs the action once per key: a repeat waits for the first attempt and gets the
     * same result. A failed attempt is forgotten, so it can be tried again.
     */
    @SuppressWarnings("unchecked")
    public <T> T runOnce(String key, Supplier<T> action) {
        Instant now = Instant.now();
        attempts.values().removeIf(attempt -> attempt.startedAt().plus(keyLifetime).isBefore(now));

        CompletableFuture<Object> result = new CompletableFuture<>();
        Attempt previous = attempts.putIfAbsent(key, new Attempt(result, now));
        if (previous != null) {
            return (T) waitFor(previous.result());
        }

        try {
            T value = action.get();
            result.complete(value);
            return value;
        } catch (RuntimeException | Error failure) {
            attempts.remove(key);
            result.completeExceptionally(failure);
            throw failure;
        }
    }

    private Object waitFor(CompletableFuture<Object> result) {
        try {
            return result.join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof ApiException apiError ? apiError : e;
        }
    }
}
