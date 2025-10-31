package com.microservices.core.product_service.services;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.product.ProductService;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.api.api.exceptions.NotFoundException;
import com.example.util.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;


    // constructor
    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }


    @Override
    public Product getProduct(int productId) {

        LOG.debug("/product return the found product for productId={}", productId);

        // if product count id less than 1 return invalid input
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        // product id = 13 means there is no product with that id.
        if (productId == 13) {
            throw new NotFoundException("No product found for productId: " + productId);
        }

        return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
    }
}
