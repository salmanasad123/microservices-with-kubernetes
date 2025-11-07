package com.example.api.api.composite.product;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Describing a restful api in the java interface instead of directly in the java class
 * is a good way of separating the API definition from its implementation.
 * We are using java interfaces to describe restful Apis and model classes to describe the data the
 * api uses.
 */

public interface ProductCompositeService {

    /**
     * Sample usage: "curl $HOST:$PORT/product-composite/1".
     *
     * @param productId Id of the product
     * @return the composite product info, if found, else null
     */

    @GetMapping(value = "/product-composite/{productId}", produces = "application/json")
    Mono<ProductAggregate> getProduct(@PathVariable(value = "productId") int productId);

    /**
     * Sample usage, see below.
     *
     * curl -X POST $HOST:$PORT/product-composite \
     *   -H "Content-Type: application/json" --data \
     *   '{"productId":123,"name":"product 123","weight":123}'
     *
     * @param body A JSON representation of the new composite product
     */
    @PostMapping(value = "/product-composite", consumes = "application/json")
    Mono<Void> createProduct(@RequestBody ProductAggregate body);

    @DeleteMapping(value = "/product-composite/{productId}")
    Mono<Void> deleteProduct(@PathVariable int productId);
}
