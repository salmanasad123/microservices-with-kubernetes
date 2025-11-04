package com.example.api.api.core.product;

import org.springframework.web.bind.annotation.*;

/**
 * Describing a restful api in the java interface instead of directly in the java class
 * is a good way of separating the API definition from its implementation.
 * We are using java interfaces to describe restful Apis and model classes to describe the data the
 * api uses.
 */

public interface ProductService {

    // the {productId} path variable maps to the variable int productId.
    // For example, an HTTP GET request to / product/123 will result in the getProduct() method being
    // called with the productId parameter set to 123.

    @GetMapping(value = "/product/{productId}", produces = "application/json")
    Product getProduct(@PathVariable(value = "productId") int productId);

    /**
     * Sample usage: "curl $HOST:$PORT/product/1".
     *
     * @param {productId} Id of the product
     * @return the product, if found, else null
     */
    @PostMapping(value = "/product", consumes = "application/json", produces = "application/json")
    Product createProduct(@RequestBody Product body);

    /**
     * Sample usage: "curl -X DELETE $HOST:$PORT/product/1".
     *
     * @param productId Id of the product
     */


    //  The implementation of the delete operation will be idempotent; that is, it will return the
    // same result if called several times. This is a valuable characteristic in fault scenarios. For
    // example, if a client experiences a network timeout during a call to a delete operation, it
    // can simply call the delete operation again without worrying about varying responses, for
    // example, OK (200) in response the first time and Not Found (404) in response to consecutive calls,
    // or any unexpected side effects. This implies that the operation should return
    // the status code OK (200) even though the entity no longer exists in the database.
    @DeleteMapping(value = "/product/{productId}")
    void deleteProduct(@PathVariable(value = "productId") int productId);

}
