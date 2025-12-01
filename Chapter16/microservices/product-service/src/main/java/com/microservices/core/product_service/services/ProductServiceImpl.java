package com.microservices.core.product_service.services;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.product.ProductService;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.api.api.exceptions.NotFoundException;
import com.example.util.util.ServiceUtil;
import com.microservices.core.product_service.persistence.ProductEntity;
import com.microservices.core.product_service.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;

import static java.util.logging.Level.FINE;

/**
 * Reactive Flow Summary:
 * Reactive Repository calls are non-blocking.
 * When you call something like repository.findById(id), it doesn’t run the DB query immediately.
 * It only defines a pipeline — a plan of what to do when data becomes available.
 * Execution starts only when someone subscribes.
 * The repository (Publisher) doesn’t do anything until a Subscriber appears.
 * Once a subscriber subscribes, the DB query actually runs.
 * The DB call is asynchronous.
 * The call is sent to the database using a reactive driver (like R2DBC).
 * The thread becomes free immediately — it doesn’t wait for the DB to respond.
 * When the DB responds, it emits signals:
 * onNext() → when data (like a Product) is ready
 * onComplete() → when the operation finishes successfully
 * onError() → if something goes wrong
 * These signals travel through the reactive pipeline you defined (like .map(), .log(), etc.).
 * The Subscriber receives and processes these signals.
 * The subscriber reacts to data, completion, or errors as they happen — without blocking any thread.
 * In a Spring WebFlux app, the framework itself becomes the Subscriber.
 * When a request hits your controller, Spring WebFlux subscribes to the Mono/Flux you returned.
 * It waits for the signals (onNext, onComplete, onError) and then:
 * Converts the result into an HTTP response,
 */

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


    /**
     * The product microservice implements the actual delay and random error generator in the
     * ProductServiceImpl class by extending the existing stream used to read product information from
     * the MongoDB database.
     * Adding the following for Circuit Breaker:
     * The composite product API will simply pass on the parameters to the product API. The following query
     * parameters have been added to the two APIs:
     *  • delay: Causes the getProduct API on the product microservice to delay its response. The parameter
     *  is specified in seconds. For example, if the parameter is set to 3, it will cause a delay of
     *  three seconds before the response is returned.
     *  • faultPercentage: Causes the getProduct API on the product microservice to throw an exception
     *  randomly with the probability specified by the query parameter, from 0 to 100%. For example,
     *  if the parameter is set to 25, it will cause every fourth call to the API, on average, to fail
     * with an exception. It will return an HTTP error 500 (Internal Server Error) in these cases
     */
    @Override
    public Mono<Product> getProduct(int productId, int delay, int faultPercent) {

        LOG.debug("/product return the found product for productId={}", productId);

        // if product count id less than 1 return invalid input
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        // The method will return a Mono object; the processing is only declared here. The processing
        // is triggered by the web framework, Spring WebFlux, subscribing to the Mono object once it
        // receives a request to this service.
        // A product will be retrieved using its productId from the underlying database using the
        // findByProductId() method in the persistence repository.
        // If no product is found for the given productId, a NotFoundException will be thrown.
        // The log method will produce log output.
        // The mapper.entityToApi() method will be called to transform the returned entity from the
        // persistence layer into an API model object
        // Before the product information is returned, the serviceUtil object is used to fill in the currently
        // used address of the microservice.
        // Jab HTTP request aati hai, to Spring WebFlux framework khud subscriber ban jaata hai.
        // Wo automatically subscribe karta hai tumhare returned Mono ko:
        // Data receive karta hai (onNext)
        // Response serialize karta hai (onComplete)
        // Error handle karta hai (onError)
        // Jab tu Mono.error(new NotFoundException(...)) emit karta hai, to wo normal exception throw nahi karta,
        // balki wo ek reactive error signal emit karta hai —
        // aur Spring WebFlux is signal ko catch karke Global Exception Handler tak forward karta hai.
        // When the stream returns a response from the Spring Data repository, it first applies the
        // throwErrorIfBadLuck method to see whether an exception needs to be thrown. Next, it applies a
        // delay using the delayElement function in the Mono class
        Mono<Product> productEntity = productRepository.findByProductId(productId)
                .map(e -> throwErrorIfBadLuck(e, faultPercent))
                .delayElement(Duration.ofSeconds(delay))
                .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
                .log(LOG.getName(), FINE)
                .map((ProductEntity p) -> {
                    return mapper.entityToApi(p);
                })
                .map((Product p) -> {
                    return setServiceAddress(p);
                });

        return productEntity;
    }

    // The createProduct method used the save method in the repository to store a new entity. It should
    // be noted that the mapper class is used to convert Java beans between an API model class and an
    // entity class using the two mapper methods, apiToEntity() and entityToApi(). The only error we
    // handle for the create method is the DuplicateKeyException exception, which we convert into an
    // InvalidInputException exception. Api model class can also be called a DTO.
    @Override
    public Mono<Product> createProduct(Product body) {

        // Note from the preceding code that the onErrorMap() method is used to map the DuplicateKeyException
        // persistence exception to our own InvalidInputException exception.

        ProductEntity productEntity = mapper.apiToEntity(body);
        Mono<Product> newEntity = productRepository.save(productEntity)
                .log(LOG.getName(), FINE)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        (DuplicateKeyException ex) -> {
                            return new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
                        })
                .map((ProductEntity e) -> {
                    return mapper.entityToApi(e);
                });
        return newEntity;

    }

    @Override
    public Mono<Void> deleteProduct(int productId) {

        // first we find the product and if it is found we delete it using the ifPresent().
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        return productRepository.findByProductId(productId)
                .log(LOG.getName(), FINE)
                .map((ProductEntity e) -> {
                    return productRepository.delete(e);
                }).flatMap((Mono<Void> e) -> {
                    // flatMap ye Mono<Mono<Void>> ko flatten kar deta hai into a single Mono<Void>
                    return e;
                });
    }


    private Product setServiceAddress(Product product) {
        product.setServiceAddress(serviceUtil.getServiceAddress());
        return product;
    }

    private ProductEntity throwErrorIfBadLuck(ProductEntity entity, int faultPercent) {

        if (faultPercent == 0) {
            return entity;
        }

        int randomThreshold = getRandomNumber(1, 100);

        if (faultPercent < randomThreshold) {
            LOG.debug("We got lucky, no error occurred, {} < {}", faultPercent, randomThreshold);
        } else {
            LOG.info("Bad luck, an error occurred, {} >= {}", faultPercent, randomThreshold);
            throw new RuntimeException("Something went wrong...");
        }

        return entity;
    }

    private final Random randomNumberGenerator = new Random();

    private int getRandomNumber(int min, int max) {

        if (max < min) {
            throw new IllegalArgumentException("Max must be greater than min");
        }

        return randomNumberGenerator.nextInt((max - min) + 1) + min;
    }
}
