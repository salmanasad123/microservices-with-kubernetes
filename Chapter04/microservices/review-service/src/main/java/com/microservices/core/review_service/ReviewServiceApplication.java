package com.microservices.core.review_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * To enable Spring Boot’s autoconfiguration feature to detect Spring Beans in the api and util
 * projects, we also need to add a @ComponentScan annotation to the main application class, which
 * includes the packages of the api and util projects
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "com.microservices"})
public class ReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewServiceApplication.class, args);
    }

}
