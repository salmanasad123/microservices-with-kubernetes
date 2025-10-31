package com.example.api.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *  Describing a restful api in the java interface instead of directly in the java class
 *  is a good way of separating the API definition from its implementation.
 *  We are using java interfaces to describe restful Apis and model classes to describe the data the
 *  api uses.
 */

public interface ProductService {

    // the {productId} path variable maps to the variable int productId.
    // For example, an HTTP GET request to / product/123 will result in the getProduct() method being
    // called with the productId parameter set to 123.

    @GetMapping(value = "/product/{productId}", produces = "application/json")
    Product getProduct(@PathVariable(value = "productId") int productId);
}
