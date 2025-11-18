package com.microservices.core.product_composite_service.services;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.product.ProductService;
import com.example.api.api.core.recommendation.Recommendation;
import com.example.api.api.core.recommendation.RecommendationService;
import com.example.api.api.core.review.Review;
import com.example.api.api.core.review.ReviewService;
import com.example.api.api.event.Event;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.api.api.exceptions.NotFoundException;
import com.example.util.util.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static com.example.api.api.event.Event.Type.CREATE;
import static com.example.api.api.event.Event.Type.DELETE;
import static java.util.logging.Level.FINE;
import static org.springframework.http.HttpMethod.GET;

/**
 * This is the the integration component, this class is marked as spring bean using the @Component annotation
 * and implements the three core services’ API interfaces.
 * The product-composite service contains an Integration layer used to handle the communication
 * with the three core microservices. The core microservices will all have a Persistence layer used for
 * communicating with their databases.
 * <p>
 * <p>
 * The create, read, and delete services exposed by the product composite microservice will be
 * based on non-blocking synchronous APIs. The composite microservice is assumed to have
 * clients on both web and mobile platforms, as well as clients coming from other organizations
 * rather than the ones that operate the system landscape. Therefore, synchronous APIs seem
 * like a natural match.
 * <p>
 * The create and delete services provided by the core microservices will be developed as
 * event-driven asynchronous services, meaning that they will listen for create and delete events
 * on topics dedicated to each microservice.
 * <p>
 * The synchronous APIs provided by the composite microservices to create and delete aggregated
 * product information will publish create and delete events on these topics. If the publish operation succeeds,
 * it will return with a 202 (Accepted) response; otherwise, an error response will
 * be returned. The 202 response differs from a normal 200 (OK) response – it indicates that the
 * request has been accepted, but not fully processed. Instead, the processing will be completed
 * asynchronously and independently of the 202 response
 */

@Component
public class ProductCompositeIntegration implements ProductService, ReviewService, RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);
    private final RestTemplate restTemplate;

    // In the ProductCompositeIntegration integration class, we have replaced the blocking HTTP client,
    // RestTemplate, with a non-blocking HTTP client, WebClient, that comes with Spring 5.
    private final WebClient webClient;

    // Object Mapper is a JSON mapper, which is used for accessing error messages in case of errors,
    // and the configuration values that we have set up in the property file.
    private final ObjectMapper mapper;

//    private  String productServiceUrl;
//    private  String recommendationServiceUrl;
//    private  String reviewServiceUrl;

    private final StreamBridge streamBridge;

    private final Scheduler publishEventScheduler;

    // The corresponding code in the integration class, ProductCompositeIntegration, that handled
    // the hardcoded configuration is simplified and replaced by a declaration of the base URLs to
    // the APIs of the core microservices.
    // The hostnames in the preceding URLs are not actual DNS names. Instead, they are the virtual
    // hostnames that are used by the microservices when they register themselves to the Eureka
    // server, in other words, the values of the spring.application.name property.
    // Ex: http://product koi real hostname nahi hai (jaise localhost ya 192.168.x.x). Ye dummy logical names hain.
    // Main apna naam product ke naam se register kar raha hoon Eureka pe. in other words, the values of the
    // spring.application.name property.
    private static final String PRODUCT_SERVICE_URL = "http://product";
    private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
    private static final String REVIEW_SERVICE_URL = "http://review";

    // the values app.product-service.host is specified in the application.yml file for composite-service
    // the @Value annotation is used to read and inject the property value defined in yaml file.
    // In the constructor, the WebClient is auto-injected. We build the WebClient instance without any
    // configuration: If customization is required, for example, setting up common headers or filters,
    // it can be done using the builder.
    @Autowired
    public ProductCompositeIntegration(RestTemplate restTemplate, ObjectMapper mapper,
//                                       @Value("${app.product-service.host}") String productServiceHost,
//                                       @Value("${app.product-service.port}") int productServicePort,
//                                       @Value("${app.recommendation-service.host}") String recommendationServiceHost,
//                                       @Value("${app.recommendation-service.port}") int recommendationServicePort,
//                                       @Value("${app.review-service.host}") String reviewServiceHost,
//                                       @Value("${app.review-service.port}") int reviewServicePort,
                                       WebClient.Builder webClient, StreamBridge streamBridge,
                                       @Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {

        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.webClient = webClient.build();
        this.streamBridge = streamBridge;
        this.publishEventScheduler = publishEventScheduler;
//        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product";
//        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation";
//        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review";
    }


    @Override
    public Mono<Product> getProduct(int productId) {

        try {
            String url = PRODUCT_SERVICE_URL + "/product/" + productId;
            LOG.debug("Will call getProduct API on URL: {}", url);

            // We are using restTemplate to make the actual api calls to other core services.
            // The expected response is a Product object. It can be expressed in the call to getForObject() by specifying the Product.class class that RestTemplate will
            // map the JSON response to. For Reactive calls we will use WebClient
            Mono<Product> product = webClient.get().uri(url).retrieve()
                    .bodyToMono(Product.class)
                    .log(LOG.getName(), Level.FINE);

            return product;

        } catch (WebClientResponseException ex) {
            switch (HttpStatus.resolve(ex.getStatusCode().value())) {
                case NOT_FOUND:
                    throw new NotFoundException(getErrorMessage(ex));
                case UNPROCESSABLE_ENTITY:
                    throw new InvalidInputException(getErrorMessage(ex));
                default:
                    LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                    LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                    throw ex;
            }
        }
    }

    // It simply delegates the responsibility of sending the HTTP request to the RestTemplate object and
    // delegates error handling to the helper method, handleHttpClientException.

    // When the composite service receives HTTP requests for the creation and deletion of composite products,
    // it will publish the corresponding events to the core services on their topics. To be able to publish
    // events in the composite service, we need to perform the following steps:
    // 1. Publish events in the integration layer
    // 2. Add configuration for publishing events
    // 3. Change tests so that they can test the publishing of events
    @Override
    public Mono<Product> createProduct(Product body) {

        // Use the helper class StreamBridge to publish the event on the desired topic.
        // Since the sendMessage() uses blocking code, when calling streamBridge, it is executed on a
        // thread provided by a dedicated scheduler, publishEventScheduler. This is the same approach
        // as for handling blocking JPA code in the review microservice
        Mono<Product> productMono = Mono.fromCallable(() -> {
            sendMessage("products-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);

        return productMono;
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {

        Mono<Void> voidMono = Mono.fromRunnable(() -> {
            sendMessage("products-out-0", new Event(DELETE, productId, null));
        }).subscribeOn(publishEventScheduler).then();

        return voidMono;
    }

    /**
     * If the API call to the product service fails with an HTTP error response, the whole API request will fail.
     * The onErrorMap() method in WebClient will call our handleException(ex) method, which maps the
     * HTTP exceptions thrown by the HTTP layer to our own exceptions, for example, a NotFoundException
     * or a InvalidInputException.
     * However, if calls to the product service succeed but the call to either the recommendation or review
     * API fails, we don’t want to let the whole request fail. Instead, we want to return as much information
     * as is available back to the caller. Therefore, instead of propagating an exception in these cases, we
     * will instead return an empty list of recommendations or reviews. To suppress the error, we will make
     * the call onErrorResume(error -> empty())
     */
    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        try {
            // construct the url, and make the api call through restTemplate
            String url = RECOMMENDATION_SERVICE_URL + "/recommendation?productId=" + productId;

            LOG.debug("Will call getRecommendations API on URL: {}", url);
            Flux<Recommendation> recommendationFlux = webClient.get().uri(url).retrieve()
                    .bodyToFlux(Recommendation.class)
                    .log(LOG.getName(), Level.FINE);

            return recommendationFlux;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {

        Mono<Recommendation> recommendationMono = Mono.fromCallable(() -> {
            sendMessage("recommendations-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);

        return recommendationMono;
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {

        return Mono.fromRunnable(() -> {
                    sendMessage("recommendations-out-0", new Event(DELETE, productId, null));
                })
                .subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Flux<Review> getReviews(int productId) {

        try {
            String url = REVIEW_SERVICE_URL + "/review?productId=" + productId;

            LOG.debug("Will call getReviews API on URL: {}", url);

            Flux<Review> reviewFlux = webClient.get().uri(url).retrieve()
                    .bodyToFlux(Review.class)
                    .log(LOG.getName(), Level.FINE);

            return reviewFlux;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public Mono<Review> createReview(Review body) {

        return Mono.fromCallable(() -> {
            sendMessage("reviews-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        return Mono.fromRunnable(() -> {
                    sendMessage("reviews-out-0", new Event(DELETE, productId, null));
                })
                .subscribeOn(publishEventScheduler).then();
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    /**
     * The helper method, sendMessage(), creates a Message object and sets the payload and the
     * partitionKey header as described above. Finally, it uses the streamBridge object to send the
     * event to the messaging system, which will publish it on the topic defined in the configuration
     *
     * @param bindingName
     * @param event
     */
    private void sendMessage(String bindingName, Event event) {
        LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
