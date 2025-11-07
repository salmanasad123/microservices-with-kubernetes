package com.example.api.api.core.recommendation;

import jakarta.websocket.server.PathParam;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RecommendationService {

    /**
     *
     * The read services provided by the core microservices will also be developed as non-blocking
     * synchronous APIs since there is an end user waiting for their responses.
     *
     * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
     *
     * @param productId Id of the product
     * @return the recommendations of the product
     */
    @GetMapping(value = "/recommendation", produces = "application/json")
    public Flux<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);

    /**
     * The create and delete services provided by the core microservices will be developed as
     * event-driven asynchronous services, meaning that they will listen for create and delete events
     * on topics dedicated to each microservice.
     *
     * Sample usage, see below.
     * <p>
     * curl -X POST $HOST:$PORT/recommendation \
     * -H "Content-Type: application/json" --data \
     * '{"productId":123,"recommendationId":456,"author":"me","rate":5,"content":"yada, yada, yada"}'
     *
     * @param body A JSON representation of the new recommendation
     * @return A JSON representation of the newly created recommendation
     */
    @PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
    Mono<Recommendation> createRecommendation(@RequestBody Recommendation body);

    /**
     * The create and delete services provided by the core microservices will be developed as
     * event-driven asynchronous services, meaning that they will listen for create and delete events
     * on topics dedicated to each microservice.
     *
     * Sample usage: "curl -X DELETE $HOST:$PORT/recommendation?productId=1".
     *
     * @param productId Id of the product
     */
    @DeleteMapping(value = "/recommendation")
    Mono<Void> deleteRecommendations(@RequestParam(value = "productId", required = true) int productId);
}
