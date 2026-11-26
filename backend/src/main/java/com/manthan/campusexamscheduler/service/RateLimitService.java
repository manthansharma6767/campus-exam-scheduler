package com.manthan.campusexamscheduler.service;

import com.manthan.campusexamscheduler.entity.AuthRateLimit;
import com.manthan.campusexamscheduler.exception.RateLimitExceededException;
import com.manthan.campusexamscheduler.repository.AuthRateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Failure limiter for public authentication flows. State lives in MySQL so
 * every application instance enforces the same limits.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final AuthRateLimitRepository rateLimitRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void check(String key) {
        Instant now = Instant.now();
        AuthRateLimit entry = rateLimitRepository.findById(key).orElse(null);
        if (entry == null) {
            return;
        }
        if (isExpired(entry, now)) {
            rateLimitRepository.deleteByRateKey(key);
            return;
        }
        if (entry.getFailureCount() >= MAX_FAILURES) {
            throw new RateLimitExceededException(retryAfterSeconds(entry, now));
        }
    }

    /**
     * Runs in its own transaction because callers often throw immediately after
     * recording a failure; the counter must survive that rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String key) {
        Instant now = Instant.now();
        rateLimitRepository.recordFailure(key, now, now.minus(WINDOW));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(String key) {
        rateLimitRepository.deleteByRateKey(key);
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void deleteExpiredAttempts() {
        rateLimitRepository.deleteExpired(Instant.now().minus(WINDOW));
    }

    private boolean isExpired(AuthRateLimit entry, Instant now) {
        return !now.isBefore(entry.getWindowStartedAt().plus(WINDOW));
    }

    private long retryAfterSeconds(AuthRateLimit entry, Instant now) {
        Duration remaining = Duration.between(now, entry.getWindowStartedAt().plus(WINDOW));
        return Math.max(1, (remaining.toMillis() + 999) / 1000);
    }
}
