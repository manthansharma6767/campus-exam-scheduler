package com.manthan.campusexamscheduler;

import java.net.URI;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Shared MySQL test configuration. The schema check runs while Spring builds
 * the test context, before it can connect to or modify a database.
 */
final class MySqlTestProperties {

    private static final String TEST_SCHEMA = "campus_exam_scheduler";
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC";

    private MySqlTestProperties() {
    }

    static void register(DynamicPropertyRegistry registry) {
        String url = configured("TEST_DATASOURCE_URL", DEFAULT_URL);
        requireDedicatedTestSchema(url);

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("JWT_SECRET", () -> "integration-test-jwt-secret-with-at-least-32-characters");
        registry.add("ADMIN_SETUP_TOKEN", () -> "workflow-test-setup-token");
    }

    private static String configured(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    static void requireDedicatedTestSchema(String url) {
        try {
            if (url == null || !url.startsWith("jdbc:mysql://")) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(url.substring("jdbc:".length()));
            String path = uri.getPath();
            String schema = path == null ? "" : path.replaceFirst("^/", "");
            if (!"mysql".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !TEST_SCHEMA.equals(schema)
                    || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Spring integration tests require a MySQL URL targeting the dedicated "
                            + TEST_SCHEMA + " schema, with credentials supplied separately.",
                    exception);
        }
    }
}