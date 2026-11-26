package com.manthan.campusexamscheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "auth_rate_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRateLimit {

    @Id
    @Column(name = "rate_key", length = 200)
    private String rateKey;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

}
