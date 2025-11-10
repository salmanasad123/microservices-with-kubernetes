package com.microservices.core.recommendation_service.services;

import com.example.api.api.core.recommendation.Recommendation;
import com.example.api.api.core.recommendation.RecommendationService;
import com.example.api.api.event.Event;
import com.example.api.api.exceptions.EventProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
/**
 * Normal situation mein agar tu RabbitMQ ya Kafka se messages consume karna chahta hai, to tujhe khud likhna padta hai:
 * Connection create karna
 * Channel open karna
 * Exchange/Queue declare karna
 * Listener thread lagana
 * Retry aur DLQ handle karna
 * Yani tu pure Rabbit/Kafka API ke sath kaam karta hai (bohot boilerplate code).
 *
 * 🚀 Ab aata hai Spring Cloud Stream:
 * Spring Cloud Stream = abstraction layer (framework)
 * jo messaging system (RabbitMQ, Kafka, etc.) ke sath kaam karna bohot easy aur uniform bana deta hai.
 *
 * The class is annotated with @Configuration, telling Spring to look for Spring beans in the class
 *
 * The REST APIs for creating and deleting entities have been replaced with a message processor in each
 * core microservice that consumes create and delete events on each entity’s topic. To be able to consume
 * messages that have been published to a topic, we need to declare a Spring Bean that implements the
 * functional interface java.util.function.Consumer.
 * <p>
 * The message processor for the recommendation service is declared as:
 */

@Configuration
public class MessageProcessorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

    private final RecommendationService recommendationService;

    @Autowired
    public MessageProcessorConfig(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Bean
    public Consumer<Event<Integer, Recommendation>> messageProcessor() {
        return event -> {

            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    Recommendation recommendation = event.getData();
                    LOG.info("Create recommendation with ID: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
                    recommendationService.createRecommendation(recommendation).block();
                    break;

                case DELETE:
                    int productId = event.getKey();
                    LOG.info("Delete recommendations with ProductID: {}", productId);
                    recommendationService.deleteRecommendations(productId).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    LOG.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }

            LOG.info("Message processing done!");
        };

    }
}
