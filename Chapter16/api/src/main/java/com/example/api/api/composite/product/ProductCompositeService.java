package com.example.api.api.composite.product;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Describing a restful api in the java interface instead of directly in the java class
 * is a good way of separating the API definition from its implementation.
 * We are using java interfaces to describe restful Apis and model classes to describe the data the
 * api uses.
 *
 * The create, read, and delete services exposed by the product composite microservice will be
 * based on non-blocking synchronous APIs. The composite microservice is assumed to have
 * clients on both web and mobile platforms, as well as clients coming from other organizations
 * rather than the ones that operate the system landscape. Therefore, synchronous APIs seem
 * like a natural match.
 */

public interface ProductCompositeService {

    /**
     * Sample usage: "curl $HOST:$PORT/product-composite/1".
     *
     * @param productId Id of the product
     * @return the composite product info, if found, else null
     */

    @GetMapping(value = "/product-composite/{productId}", produces = "application/json")
    Mono<ProductAggregate> getProduct(@PathVariable(value = "productId") int productId,
                                      @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
                                      @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent);

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
