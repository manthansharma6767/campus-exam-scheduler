package com.manthan.campusexamscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:campus_exam_scheduler_test;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CampusExamSchedulerApplicationTests {

    @Test
    void contextLoads() {
    }

}
