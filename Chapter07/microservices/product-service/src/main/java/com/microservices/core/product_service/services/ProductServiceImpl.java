package com.microservices.core.product_service.services;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.product.ProductService;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.api.api.exceptions.NotFoundException;
import com.example.util.util.ServiceUtil;
import com.microservices.core.product_service.persistence.ProductEntity;
import com.microservices.core.product_service.persistence.ProductRepository;
import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;

    private final ProductRepository productRepository;

    private final ProductMapper mapper;

    // constructor
    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository productRepository,
                              ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }


    @Override
    public Product getProduct(int productId) {

        LOG.debug("/product return the found product for productId={}", productId);

        // if product count id less than 1 return invalid input
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        // Since the repository method returns an Optional product, we can use the orElseThrow() method in the
        // Optional class to conveniently throw a NotFoundException exception if no product entity is found.
        // Before the product information is returned, the serviceUtil object is used to fill in the currently
        // used address of the microservice.
        ProductEntity productEntity = productRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    throw new NotFoundException("No product found for productId: " + productId);
                });

        Product product = mapper.entityToApi(productEntity);
        product.setServiceAddress(serviceUtil.getServiceAddress());

        return product;
    }

    // The createProduct method used the save method in the repository to store a new entity. It should
    // be noted that the mapper class is used to convert Java beans between an API model class and an
    // entity class using the two mapper methods, apiToEntity() and entityToApi(). The only error we
    // handle for the create method is the DuplicateKeyException exception, which we convert into an
    // InvalidInputException exception. Api model class can also be called a DTO.
    @Override
    public Product createProduct(Product body) {
        try {
            ProductEntity productEntity = mapper.apiToEntity(body);
            ProductEntity newEntity = productRepository.save(productEntity);
            return mapper.entityToApi(newEntity);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
        }
    }

    @Override
    public void deleteProduct(int productId) {

        // first we find the product and if it is found we delete it using the ifPresent().
        productRepository.findByProductId(productId)
                .ifPresent((ProductEntity entity) -> {
                    productRepository.delete(entity);
                });
    }
}
