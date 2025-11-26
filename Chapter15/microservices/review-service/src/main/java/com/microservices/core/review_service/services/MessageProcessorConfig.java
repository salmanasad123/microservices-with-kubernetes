package com.microservices.core.review_service.services;


import com.example.api.api.core.review.Review;
import com.example.api.api.core.review.ReviewService;
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
 * <p>
 * 🚀 Ab aata hai Spring Cloud Stream:
 * Spring Cloud Stream = abstraction layer (framework)
 * jo messaging system (RabbitMQ, Kafka, etc.) ke sath kaam karna bohot easy aur uniform bana deta hai.
 * <p>
 * The class is annotated with @Configuration, telling Spring to look for Spring beans in the class
 * <p>
 * The REST APIs for creating and deleting entities have been replaced with a message processor in each
 * core microservice that consumes create and delete events on each entity’s topic. To be able to consume
 * messages that have been published to a topic, we need to declare a Spring Bean that implements the
 * functional interface java.util.function.Consumer.
 * <p>
 * The message processor for the review service is declared as:
 */

@Configuration
public class MessageProcessorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

    private final ReviewService reviewService;

    @Autowired
    public MessageProcessorConfig(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Bean
    public Consumer<Event<Integer, Review>> messageProcessor() {
        return event -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {

                case CREATE:
                    Review review = event.getData();
                    LOG.info("Create review with ID: {}/{}", review.getProductId(), review.getReviewId());
                    reviewService.createReview(review).block();
                    break;

                case DELETE:
                    int productId = event.getKey();
                    LOG.info("Delete reviews with ProductID: {}", productId);
                    reviewService.deleteReviews(productId).block();
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
