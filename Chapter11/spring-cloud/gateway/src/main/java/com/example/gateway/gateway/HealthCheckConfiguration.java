package com.example.gateway.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.logging.Level.FINE;

/**
 * Since the edge server will handle all incoming traffic, we will move the composite health
 * check from the product composite service to the edge server. The class was in product-composite service package.
 * This can be seen in previous chapter09.The class is now moved to the gateway.
 * With an edge server in place, external health check requests also have to go through the edge server.
 * Therefore, the composite health check that checks the status of all microservices has been moved
 * from the product-composite service to the edge server.
 */
@Configuration
public class HealthCheckConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckConfiguration.class);

    private WebClient webClient;

    @Autowired
    public HealthCheckConfiguration(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Bean
    ReactiveHealthContributor healthcheckMicroservices() {

        final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();

        registry.put("product",           () -> getHealth("http://product"));
        registry.put("recommendation",    () -> getHealth("http://recommendation"));
        registry.put("review",            () -> getHealth("http://review"));
        registry.put("product-composite", () -> getHealth("http://product-composite"));

        return CompositeReactiveHealthContributor.fromMap(registry);
    }

    private Mono<Health> getHealth(String baseUrl) {
        String url = baseUrl + "/actuator/health";
        LOG.debug("Setting up a call to the Health API on URL: {}", url);
        return webClient.get().uri(url).retrieve().bodyToMono(String.class)
                .map((String s) -> {
                    return new Health.Builder().up().build();
                })
                .onErrorResume((Throwable ex) -> {
                    return Mono.just(new Health.Builder().down(ex).build());
                })
                .log(LOG.getName(), FINE);
    }
}
