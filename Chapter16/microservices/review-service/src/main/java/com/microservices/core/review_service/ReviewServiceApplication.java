package com.microservices.core.review_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * To enable Spring Boot’s autoconfiguration feature to detect Spring Beans in the api and util
 * projects, we also need to add a @ComponentScan annotation to the main application class, which
 * includes the packages of the api and util projects
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "com.microservices"})
public class ReviewServiceApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

    private final Integer threadPoolSize;
    private final Integer taskQueueSize;

    /**
     * In the case of the review service, which uses JPA to access its data in a relational database, we don’t
     * have support for a non-blocking programming model. Instead, we can run the blocking code using
     * a Scheduler, which is capable of running the blocking code on a thread from a dedicated thread
     * pool with a limited number of threads. Using a thread pool for the blocking code avoids draining the
     * available threads in the microservice and avoids affecting concurrent non-blocking processing in the
     * microservice, if there is any
     * @param threadPoolSize
     * @param taskQueueSize
     */
    @Autowired
    public ReviewServiceApplication(@Value("${app.threadPoolSize:10}") Integer threadPoolSize,
                                    @Value("${app.taskQueueSize:100}") Integer taskQueueSize) {
        this.threadPoolSize = threadPoolSize;
        this.taskQueueSize = taskQueueSize;
    }

    //  we can see that the scheduler bean is named jdbcScheduler and that we can configure its thread pool
    //  using the following properties:
    // • app.threadPoolSize, specifying the max number of threads in the pool; defaults to 10
    // • app.taskQueueSize, specifying the max number of tasks that are allowed to be placed
    // in a queue waiting for available threads; defaults to 100
    @Bean
    public Scheduler jdbcScheduler() {
        LOG.info("Creates a jdbcScheduler with thread pool size = {}", threadPoolSize);
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
    }

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
