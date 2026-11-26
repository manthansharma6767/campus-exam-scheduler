package com.manthan.campusexamscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusExamSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusExamSchedulerApplication.class, args);
    }

}
