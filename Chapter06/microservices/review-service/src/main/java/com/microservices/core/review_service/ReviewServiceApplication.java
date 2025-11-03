package com.microservices.core.review_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * To enable Spring Boot’s autoconfiguration feature to detect Spring Beans in the api and util
 * projects, we also need to add a @ComponentScan annotation to the main application class, which
 * includes the packages of the api and util projects
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "com.microservices"})
public class ReviewServiceApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

    public static void main(String[] args) {
        // When scaling up the number of microservices where each microservice connects to its own database,
        // it can be hard to keep track of what database each microservice actually uses. To avoid this confusion,
        // a good practice is to add a LOG statement directly after the startup of a microservice that logs
        // connection information that is used to connect to the database
        ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);

        String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
        LOG.info("Connected to MySQL: " + mysqlUri);

    }

}
