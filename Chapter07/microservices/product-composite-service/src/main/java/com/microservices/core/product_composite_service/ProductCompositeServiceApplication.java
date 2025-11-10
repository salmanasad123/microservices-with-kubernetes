package com.microservices.core.product_composite_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * The product-composite service contains an Integration layer used to handle the communication
 * with the three core microservices. The core microservices will all have a Persistence layer used for
 * communicating with their databases
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "com.microservices"})
public class ProductCompositeServiceApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceApplication.class);
    private final Integer threadPoolSize;
    private final Integer taskQueueSize;

    public static void main(String[] args) {
        SpringApplication.run(ProductCompositeServiceApplication.class, args);
    }

    @Autowired
    public ProductCompositeServiceApplication(@Value("${app.threadPoolSize:10}") Integer threadPoolSize,
                                              @Value("${app.taskQueueSize:100}") Integer taskQueueSize) {
        this.threadPoolSize = threadPoolSize;
        this.taskQueueSize = taskQueueSize;
    }

    // The integration component uses a helper class in the Spring Framework, RestTemplate, to perform
    // the actual HTTP requests to the core microservices. Before we can inject it into the integration component,
    // we need to configure it. We do that in the main application class, ProductCompositeService Application.java
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Scheduler publishEventScheduler() {
        LOG.info("Creates a messagingScheduler with connectionPoolSize = {}", threadPoolSize);
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool");
    }
}
