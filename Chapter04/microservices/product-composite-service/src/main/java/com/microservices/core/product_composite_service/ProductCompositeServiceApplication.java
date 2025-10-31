package com.microservices.core.product_composite_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "com.microservices"})
public class ProductCompositeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCompositeServiceApplication.class, args);
    }

    // The integration component uses a helper class in the Spring Framework, RestTemplate, to perform
    // the actual HTTP requests to the core microservices. Before we can inject it into the integration component,
    // we need to configure it. We do that in the main application class, ProductCompositeService Application.java
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
