package com.microservices.core.product_service.services;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.product.ProductService;
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
 * The message processor for the product service is declared as:
 */

@Configuration
public class MessageProcessorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

    private ProductService productService;

    @Autowired
    public MessageProcessorConfig(ProductService productService) {
        this.productService = productService;
    }

    @Bean
    public Consumer<Event<Integer, Product>> messageProcessor() {
        return (Event<Integer, Product> event) -> {
            LOG.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {
                case CREATE:
                    Product product = event.getData();
                    LOG.info("Create product with ID: {}", product.getProductId());
                    // To ensure that we can propagate exceptions thrown by the productService bean back to the
                    // messaging system, we call the block() method on the responses we get back from the productService bean.
                    // This ensures that the message processor waits for the productService bean to complete its creation
                    // or deletion in the underlying database. Without calling the block() method, we would not be able
                    // to propagate exceptions and the messaging system would not be able to re-queue a failed attempt or
                    // possibly move the message to a dead-letter queue; instead, the message would silently be dropped.
                    productService.createProduct(product).block();
                    break;

                case DELETE:
                    int productId = event.getKey();
                    LOG.info("Delete product with ProductID: {}", productId);
                    productService.deleteProduct(productId).block();
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
