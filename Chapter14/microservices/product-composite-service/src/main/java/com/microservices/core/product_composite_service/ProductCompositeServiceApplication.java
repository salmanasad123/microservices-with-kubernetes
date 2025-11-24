package com.microservices.core.product_composite_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * The product-composite service contains an Integration layer used to handle the communication
 * with the three core microservices. The core microservices will all have a Persistence layer used for
 * communicating with their databases.
 *
 * This Server will be a ResourceServer.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "com.microservices"})
public class ProductCompositeServiceApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceApplication.class);

    private final Integer threadPoolSize;
    private final Integer taskQueueSize;

    @Autowired
    public ProductCompositeServiceApplication(@Value("${app.threadPoolSize:10}") Integer threadPoolSize,
                                              @Value("${app.taskQueueSize:100}") Integer taskQueueSize) {
        this.threadPoolSize = threadPoolSize;
        this.taskQueueSize = taskQueueSize;
    }



    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(ProductCompositeServiceApplication.class, args);
    }

    // The integration component uses a helper class in the Spring Framework, RestTemplate, to perform
    // the actual HTTP requests to the core microservices. Before we can inject it into the integration component,
    // we need to configure it. We do that in the main application class, ProductCompositeService Application.java
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * To be able to look up available microservice instances through the Eureka server in the product
     * composite microservice, we also need to do the following:
     * Add a Spring bean in the main application class, ProductCompositeServiceApplication, which
     * creates a load balancer-aware WebClient builder:
     *
     *
     * However, for the product-composite service, one problem remains. To ensure that a WebClient
     * instance is correctly instrumented for observation, for example, to be able to propagate the current
     * trace and span IDs as headers in an outgoing request, the WebClient.Builder instance is expected to
     * be injected using auto-wiring. Unfortunately, when using Eureka for service discovery, the WebClient.
     *  Builder instance is recommended to be created as a bean annotated with @LoadBalanced as:
     *
     *  public WebClient.Builder loadBalancedWebClientBuilder() {
     *  return WebClient.builder();
     *   }
     *  So, there is a conflict in how to create a WebClient instance when used with both Eureka and Microme
     * ter Tracing. To resolve this conflict, the @LoadBalanced bean can be replaced by a load-balancer-aware
     * exchange-filter function, ReactorLoadBalancerExchangeFilterFunction. An exchange-filter function
     * can be set on an auto-wired WebClient.Builder instance like
     */

    @Autowired
    private ReactorLoadBalancerExchangeFilterFunction lbFunction;
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.filter(lbFunction).build();
    }
    @Bean
    public Scheduler publishEventScheduler() {
        LOG.info("Creates a messagingScheduler with connectionPoolSize = {}", threadPoolSize);
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool");
    }

}
