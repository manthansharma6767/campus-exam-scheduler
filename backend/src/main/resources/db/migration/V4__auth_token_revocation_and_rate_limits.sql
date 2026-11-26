-- Rotate a per-administrator token version whenever credentials change so
-- previously issued JWTs are rejected immediately. Move public-endpoint
-- throttling into MySQL so every application instance shares the same state.

ALTER TABLE administrators
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0 AFTER password;

CREATE TABLE auth_rate_limits (
    rate_key VARCHAR(200) NOT NULL,
    failure_count INT NOT NULL,
    window_started_at DATETIME(6) NOT NULL,
    PRIMARY KEY (rate_key),
    KEY idx_auth_rate_limits_window (window_started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
