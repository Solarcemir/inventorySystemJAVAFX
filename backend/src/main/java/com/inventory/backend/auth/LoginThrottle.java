package com.inventory.backend.auth;

import com.inventory.backend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after a few wrong passwords for a username, that
 * username must wait before trying again. Unknown usernames are treated the same,
 * so the answer never reveals which users exist.
 */
@Component
public class LoginThrottle {

    private static final int FREE_ATTEMPTS = 5;
    private static final int MAX_TRACKED_USERNAMES = 10_000;

    private record Failures(int count, Instant lockedUntil) {}

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Duration lockTime;

    public LoginThrottle() {
        this(Duration.ofSeconds(30));
    }

    public LoginThrottle(Duration lockTime) {
        this.lockTime = lockTime;
    }

    public void checkAllowed(String username) {
        Failures current = failures.get(key(username));
        if (current != null && current.lockedUntil() != null && Instant.now().isBefore(current.lockedUntil())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera " + lockTime.toSeconds() + " segundos e intenta de nuevo.");
        }
    }

    public void recordFailure(String username) {
        if (failures.size() > MAX_TRACKED_USERNAMES) {
            failures.clear();
        }
        failures.compute(key(username), (name, previous) -> {
            int count = (previous != null ? previous.count() : 0) + 1;
            return new Failures(count, count >= FREE_ATTEMPTS ? Instant.now().plus(lockTime) : null);
        });
    }

    public void recordSuccess(String username) {
        failures.remove(key(username));
    }

    /** Forgets every failed attempt. */
    public void reset() {
        failures.clear();
    }

    private String key(String username) {
        return username != null ? username.trim().toLowerCase() : "";
    }
}
