package com.manthan.campusexamscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CampusExamSchedulerApplicationTests {
    @DynamicPropertySource
    static void configureMySqlTestDatabase(DynamicPropertyRegistry registry) {
        MySqlTestProperties.register(registry);
    }

    @Test
    void contextLoads() {
    }

}
