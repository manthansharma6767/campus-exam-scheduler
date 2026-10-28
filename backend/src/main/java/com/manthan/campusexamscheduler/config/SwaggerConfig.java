package com.manthan.campusexamscheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Campus Exam Scheduler API")
                                .version("1.0")
                                .description("REST API for Campus Exam Scheduler built using Spring Boot")
                                .contact(
                                        new Contact()
                                                .name("Manthan Sharma")
                                                .email("manthan@gmail.com")
                                )
                );
    }
}