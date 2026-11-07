package com.manthan.campusexamscheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MySqlTestPropertiesTest {

    @Test
    void acceptsTheConfiguredMySqlDatabase() {
        assertDoesNotThrow(() -> MySqlTestProperties.requireConfiguredSchema(
                "jdbc:mysql://localhost:3306/campus_exam_scheduler?serverTimezone=UTC"));
    }

    @Test
    void rejectsASeparateTestDatabase() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireConfiguredSchema(
                        "jdbc:mysql://localhost:3306/campus_exam_scheduler_test?serverTimezone=UTC"));
    }

    @Test
    void rejectsAnUnrelatedDatabase() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireConfiguredSchema(
                        "jdbc:mysql://localhost:3306/other_database?serverTimezone=UTC"));
    }

    @Test
    void rejectsNonMySqlDatabases() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireConfiguredSchema("jdbc:h2:mem:test"));
    }

    @Test
    void rejectsCredentialsEmbeddedInTheJdbcUrl() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireConfiguredSchema(
                        "jdbc:mysql://user:password@localhost:3306/campus_exam_scheduler"));
    }

    @Test
    void rejectsAutomaticDatabaseCreation() {
        assertThrows(IllegalStateException.class, () ->
                MySqlTestProperties.requireConfiguredSchema(
                        "jdbc:mysql://localhost:3306/campus_exam_scheduler?createDatabaseIfNotExist=true"));
    }
}
