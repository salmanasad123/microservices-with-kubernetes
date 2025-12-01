package com.microservices.core.product_service;

import com.example.api.api.core.product.Product;
import com.example.api.api.event.Event;
import com.microservices.core.product_service.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.function.Consumer;

import static com.example.api.api.event.Event.Type.CREATE;
import static com.example.api.api.event.Event.Type.DELETE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * We will focus on testing the APIs that our microservices expose; that is, we will start them up in in
 * tegration tests with their embedded web server and then use a test client to perform HTTP requests
 * and validate the responses.
 *
 * The automated integration tests on the API exposed by the three core microservices are similar, but
 * simpler since they don’t need to mock anything
 */

// When running tests on a single microservice, we don’t want to depend on having the Eureka
// server up and running. Therefore, we will disable the use of Netflix Eureka in all Spring Boot
// tests, that is, JUnit tests annotated with @SpringBootTest. This can be done by adding the
// eureka.client.enabled property and setting it to false in the annotation, like so
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"eureka.client.enabled=false"})
class ProductServiceApplicationTests extends MongoDbTestBase{

    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductRepository repository;

    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Product>> messageProcessor;

    @Test
    void getProductById() {

        int productId = 1;

        client.get()
                .uri("/product/" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.productId").isEqualTo(productId);
    }

    @Test
    void getProductInvalidParameterString() {

        client.get()
                .uri("/product/no-integer")
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(BAD_REQUEST)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/product/no-integer")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    void getProductNotFound() {

        int productIdNotFound = 13;

        client.get()
                .uri("/product/" + productIdNotFound)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }

    @Test
    void getProductInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        client.get()
                .uri("/product/" + productIdInvalid)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }


    /**
     * Note that we use the accept() method in the Consumer function interface declaration to invoke the
     * message processor. This means that we skip the messaging system in the tests and call the message
     * processor directly.
     */
    private void sendCreateProductEvent(int productId) {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        Event<Integer, Product> event = new Event(CREATE, productId, product);
        messageProcessor.accept(event);
    }
    // Note that we use the accept() method in the Consumer function interface declaration to invoke the
    // message processor. This means that we skip the messaging system in the tests and call the message
    // processor directly.
    private void sendDeleteProductEvent(int productId) {
        Event<Integer, Product> event = new Event(DELETE, productId, null);
        messageProcessor.accept(event);
    }
}
