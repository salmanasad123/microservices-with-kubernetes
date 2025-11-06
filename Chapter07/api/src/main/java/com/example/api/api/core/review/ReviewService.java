package com.example.api.api.core.review;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Describing a restful api in the java interface instead of directly in the java class
 * is a good way of separating the API definition from its implementation.
 * We are using java interfaces to describe restful Apis and model classes to describe the data the
 * api uses.
 *
 *
 * Reviews ek ek karke asynchronously aate hain, aur system har ek ko process karta hai jaise hi wo
 * milta hai — bina poora list ka wait kiye.
 *
 * Flux<Review> koi “ready-made list” nahi hoti.
 * Ye ek pipeline define karta hai:
 * Jab koi subscribe karega, tab data aana start hoga.
 * Har review ek ek karke emit hoga:
 * onNext(review1)
 * onNext(review2)
 * ...
 * onComplete()
 * So Flux acts like a Publisher that emits events.
 *
 */

public interface ReviewService {

    /**
     * Sample usage: "curl $HOST:$PORT/review?productId=1".
     *
     * @param productId Id of the product
     * @return the reviews of the product
     */

    @GetMapping(value = "/review", produces = "application/json")
    public Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

    /**
     * Sample usage, see below.
     *
     * curl -X POST $HOST:$PORT/review \
     *   -H "Content-Type: application/json" --data \
     *   '{"productId":123,"reviewId":456,"author":"me","subject":"yada, yada, yada","content":"yada, yada, yada"}'
     *
     * @param body A JSON representation of the new review
     * @return A JSON representation of the newly created review
     */
    @PostMapping(value = "/review", consumes = "application/json", produces = "application/json")
    Mono<Review> createReview(@RequestBody Review body);


    /**
     * Sample usage: "curl -X DELETE $HOST:$PORT/review?productId=1".
     *
     * @param productId Id of the product
     */
    @DeleteMapping(value = "/review")
    Mono<Void> deleteReviews(@RequestParam(value = "productId", required = true)  int productId);
}
