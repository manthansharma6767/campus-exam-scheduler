package com.manthan.campusexamscheduler.repository;

import com.manthan.campusexamscheduler.entity.AuthRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuthRateLimitRepository extends JpaRepository<AuthRateLimit, String> {

    /**
     * Atomically starts a new window or increments the current one. The upsert
     * is safe when several application instances record failures concurrently.
     */
    @Modifying
    @Query(value = """
            INSERT INTO auth_rate_limits (rate_key, failure_count, window_started_at)
            VALUES (:rateKey, 1, :now)
            ON DUPLICATE KEY UPDATE
                failure_count = IF(window_started_at <= :windowCutoff, 1, failure_count + 1),
                window_started_at = IF(window_started_at <= :windowCutoff, :now, window_started_at)
            """, nativeQuery = true)
    void recordFailure(
            @Param("rateKey") String rateKey,
            @Param("now") Instant now,
            @Param("windowCutoff") Instant windowCutoff);

    @Modifying
    @Query("delete from AuthRateLimit entry where entry.rateKey = :rateKey")
    int deleteByRateKey(@Param("rateKey") String rateKey);

    @Modifying
    @Query("delete from AuthRateLimit entry where entry.windowStartedAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);

}
