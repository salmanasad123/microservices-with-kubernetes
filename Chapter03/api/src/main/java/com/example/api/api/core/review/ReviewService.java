package com.example.api.api.core.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Describing a restful api in the java interface instead of directly in the java class
 * is a good way of separating the API definition from its implementation.
 * We are using java interfaces to describe restful Apis and model classes to describe the data the
 * api uses.
 */

public interface ReviewService {

    /**
     * Sample usage: "curl $HOST:$PORT/review?productId=1".
     *
     * @param productId Id of the product
     * @return the reviews of the product
     */

    @GetMapping(value = "/review", produces = "application/json")
    public List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
}
