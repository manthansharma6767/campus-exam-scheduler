package com.manthan.campusexamscheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlTestPropertiesTest {

    @Test
    void acceptsOnlyTheConfiguredMySqlDatabase() {
        assertDoesNotThrow(() -> MySqlTestProperties.requireDedicatedTestSchema(
                "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"));
    }

    @Test
    void rejectsAnotherDatabase() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireDedicatedTestSchema(
                        "jdbc:mysql://localhost:3306/campus_exam_scheduler_test?serverTimezone=UTC"));
    }

    @Test
    void rejectsNonMySqlDatabases() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireDedicatedTestSchema("jdbc:h2:mem:test"));
    }

    @Test
    void rejectsCredentialsEmbeddedInTheJdbcUrl() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireDedicatedTestSchema(
                        "jdbc:mysql://user:password@localhost:3306/campus_exam_scheduler"));
    }
}
