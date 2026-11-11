package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Small in-memory failure limiter for public authentication flows. It is
 * intentionally process-local; a multi-instance deployment should move this
 * state to a shared store.
 */
@Service
public class RateLimitService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void check(String key) {
        Instant now = Instant.now();
        AttemptWindow window = attempts.get(key);
        if (window == null) {
            return;
        }
        if (window.isExpired(now)) {
            attempts.remove(key, window);
            return;
        }
        if (window.failures() >= MAX_FAILURES) {
            throw new RateLimitExceededException(window.retryAfterSeconds(now));
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new AttemptWindow(1, now);
            }
            return new AttemptWindow(existing.failures() + 1, existing.windowStart());
        });
    }

    public void clear(String key) {
        attempts.remove(key);
    }

    private record AttemptWindow(int failures, Instant windowStart) {

        boolean isExpired(Instant now) {
            return !now.isBefore(windowStart.plus(WINDOW));
        }

        long retryAfterSeconds(Instant now) {
            Duration remaining = Duration.between(now, windowStart.plus(WINDOW));
            return Math.max(1, (remaining.toMillis() + 999) / 1000);
        }
    }
}
