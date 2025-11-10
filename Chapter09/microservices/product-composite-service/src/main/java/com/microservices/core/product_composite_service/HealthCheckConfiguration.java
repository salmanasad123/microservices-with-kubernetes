package com.microservices.core.product_composite_service;

import com.microservices.core.product_composite_service.services.ProductCompositeIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

public class HealthCheckConfiguration {

    @Autowired
    ProductCompositeIntegration integration;

    // Ye basically ek custom health contributor hai jo multiple health indicators ko combine karta hai.
    // Yani jab koi /actuator/health endpoint hit karega, ye teen health calls parallel run hongi aur
    // unka combined result dikhaya jaayega.
    @Bean
    ReactiveHealthContributor coreServices() {
        final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();

        registry.put("product", () -> {
            return integration.getProductHealth();
        });
        registry.put("recommendation", () -> {
            return integration.getRecommendationHealth();
        });
        registry.put("review", () -> {
            return integration.getReviewHealth();
        });

        return CompositeReactiveHealthContributor.fromMap(registry);
    }
}
