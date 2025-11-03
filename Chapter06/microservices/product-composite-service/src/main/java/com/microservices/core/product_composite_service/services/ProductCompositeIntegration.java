package com.microservices.core.product_composite_service.services;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.product.ProductService;
import com.example.api.api.core.recommendation.Recommendation;
import com.example.api.api.core.recommendation.RecommendationService;
import com.example.api.api.core.review.Review;
import com.example.api.api.core.review.ReviewService;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.api.api.exceptions.NotFoundException;
import com.example.util.util.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;

/**
 * This is the the integration component, this class is marked as spring bean using the @Component annotation
 * and implements the three core services’ API interfaces.
 * The product-composite service contains an Integration layer used to handle the communication
 * with the three core microservices. The core microservices will all have a Persistence layer used for
 * communicating with their databases
 */

@Component
public class ProductCompositeIntegration implements ProductService, ReviewService, RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);
    private final RestTemplate restTemplate;

    // Object Mapper is a JSON mapper, which is used for accessing error messages in case of errors,
    // and the configuration values that we have set up in the property file.
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    // the values app.product-service.host is specified in the application.yml file for composite-service
    // the @Value annotation is used to read and inject the property value defined in yaml file.
    @Autowired
    public ProductCompositeIntegration(RestTemplate restTemplate, ObjectMapper mapper,
                                       @Value("${app.product-service.host}") String productServiceHost,
                                       @Value("${app.product-service.port}") int productServicePort,
                                       @Value("${app.recommendation-service.host}") String recommendationServiceHost,
                                       @Value("${app.recommendation-service.port}") int recommendationServicePort,
                                       @Value("${app.review-service.host}") String reviewServiceHost,
                                       @Value("${app.review-service.port}") int reviewServicePort) {

        this.restTemplate = restTemplate;
        this.mapper = mapper;

        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation?productId=";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }


    @Override
    public Product getProduct(int productId) {

        try {
            String url = productServiceUrl + productId;
            LOG.debug("Will call getProduct API on URL: {}", url);

            // We are using restTemplate to make the actual api calls to other core services.
            // The expected response is a Product object. It can be expressed in the call to getForObject() by specifying the Product.class class that RestTemplate will
            // map the JSON response to.
            Product product = restTemplate.getForObject(url, Product.class);

            LOG.debug("Found a product with id: {}", product.getProductId());

            return product;

        } catch (HttpClientErrorException ex) {

            switch (HttpStatus.resolve(ex.getStatusCode().value())) {
                case NOT_FOUND:
                    throw new NotFoundException(getErrorMessage(ex));

                case UNPROCESSABLE_ENTITY:
                    throw new InvalidInputException(getErrorMessage(ex));

                default:
                    LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                    LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                    throw ex;
            }
        }
    }

    @Override
    public Product createProduct(Product body) {
        return null;
    }

    @Override
    public void deleteProduct(int productId) {

    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        try {
            // construct the url, and make the api call through restTemplate
            String url = recommendationServiceUrl + productId;

            LOG.debug("Will call getRecommendations API on URL: {}", url);
            List<Recommendation> recommendations = restTemplate
                    .exchange(url, GET, null, new ParameterizedTypeReference<List<Recommendation>>() {})
                    .getBody();

            LOG.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
            return recommendations;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        return null;
    }

    @Override
    public void deleteRecommendations(int productId) {

    }

    @Override
    public List<Review> getReviews(int productId) {

        try {
            String url = reviewServiceUrl + productId;

            LOG.debug("Will call getReviews API on URL: {}", url);
            /**
             * a more advanced method, exchange(), has to be used. The reason for this is the automatic mapping from
             * a JSON response to a model class that RestTemplate performs. The getRecommendations() and
             * getReviews() methods expect a generic list in the responses, that is, List<Recommendation>
             * and List<Review>. Since generics don’t hold any type of information at runtime, we can’t specify
             * that the methods expect a generic list in their responses. Instead, we can use a helper class from the Spring Framework,
             * ParameterizedTypeReference, which is designed to resolve this problem by holding the type
             * information at runtime. This means that RestTemplate can figure out what class to map the JSON responses to.
             */
            List<Review> reviews = restTemplate
                    .exchange(url, GET, null, new ParameterizedTypeReference<List<Review>>() {
                    })
                    .getBody();

            LOG.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
            return reviews;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Review createReview(Review body) {
        return null;
    }

    @Override
    public void deleteReviews(int productId) {

    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
