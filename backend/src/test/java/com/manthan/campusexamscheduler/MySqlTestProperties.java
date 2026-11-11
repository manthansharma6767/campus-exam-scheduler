package com.manthan.campusexamscheduler;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Shared MySQL test configuration. Tests target the existing
 * {@code campus_exam_scheduler} schema, never let Hibernate change its schema,
 * and rely on transactional rollback for cleanup.
 */
final class MySqlTestProperties {

    static final String TEST_SCHEMA = "campus_exam_scheduler";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC";
    private static final String TEST_JWT_SECRET = configured("TEST_JWT_SECRET", randomSecret());
    private static final String TEST_ADMIN_SETUP_TOKEN =
            configured("TEST_ADMIN_SETUP_TOKEN", randomSecret());

    private MySqlTestProperties() {
    }

    static void register(DynamicPropertyRegistry registry) {
        String url = configured("TEST_DATASOURCE_URL",
                configured("SPRING_DATASOURCE_URL", DEFAULT_URL));
        requireConfiguredSchema(url);

        String username = configured("TEST_DATASOURCE_USERNAME", null);
        if (username == null || username.isBlank()) {
            username = configured("SPRING_DATASOURCE_USERNAME", "root");
        }

        String password = configured("TEST_DATASOURCE_PASSWORD", null);
        if (password == null) {
            password = configured("SPRING_DATASOURCE_PASSWORD", "");
        }

        final String datasourceUsername = username;
        final String datasourcePassword = password;
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> datasourceUsername);
        registry.add("spring.datasource.password", () -> datasourcePassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        // The schema is owned by the application. Tests must never create, alter,
        // truncate, or drop its tables.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
        registry.add("ADMIN_SETUP_TOKEN", () -> TEST_ADMIN_SETUP_TOKEN);
    }

    static String adminSetupToken() {
        return TEST_ADMIN_SETUP_TOKEN;
    }

    private static String configured(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static void requireConfiguredSchema(String url) {
        try {
            if (url == null || !url.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(url.substring("jdbc:".length()));
            String path = uri.getPath();
            String schema = path == null ? "" : path.replaceFirst("^/", "");
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery().toLowerCase(Locale.ROOT);
            if (!"mysql".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !TEST_SCHEMA.equals(schema)
                    || uri.getRawUserInfo() != null
                    || query.contains("createdatabaseifnotexist=true")) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Spring integration tests require a MySQL URL targeting the existing "
                            + TEST_SCHEMA + " schema, with credentials supplied separately and "
                            + "without createDatabaseIfNotExist. Tests do not create or drop tables.",
                    exception);
        }
    }
}
