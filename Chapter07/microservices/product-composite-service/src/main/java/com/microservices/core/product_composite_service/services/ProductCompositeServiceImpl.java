package com.microservices.core.product_composite_service.services;

import com.example.api.api.composite.product.*;
import com.example.api.api.core.product.Product;
import com.example.api.api.core.recommendation.Recommendation;
import com.example.api.api.core.review.Review;
import com.example.api.api.exceptions.NotFoundException;
import com.example.util.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the api implementation class. In the same way that we did for the core services, the composite service
 * implements its API interface, ProductCompositeService, and is annotated with @RestController to mark it as
 * a REST service
 */

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);
    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {

        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    /**
     * The integration component is used to call the three core services, and a helper method,
     * createProductAggregate(), is used to create a response object of the ProductAggregate type based
     * on the responses from the calls to the integration component.
     */
    @Override
    public ProductAggregate getProduct(int productId) {

        LOG.debug("getCompositeProduct: lookup a product aggregate for productId: {}", productId);

        Product product = integration.getProduct(productId);
        if (product == null) {
            throw new NotFoundException("No product found for productId: " + productId);
        }

        List<Recommendation> recommendations = integration.getRecommendations(productId);

        List<Review> reviews = integration.getReviews(productId);

        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
    }

    /**
     * The composite create method will split up the aggregate product object into discrete objects for product,
     * recommendation, and review and call the corresponding create methods in the integration layer
     * @param body A JSON representation of the new composite product
     */
    @Override
    public void createProduct(ProductAggregate body) {

        try {
            LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(product);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach((RecommendationSummary recommendationSummary) -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(),
                            recommendationSummary.getRecommendationId(), recommendationSummary.getAuthor(),
                            recommendationSummary.getRate(), recommendationSummary.getContent(),
                            null);
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach((ReviewSummary reviewSummary) -> {
                    Review review = new Review(body.getProductId(), reviewSummary.getReviewId(), reviewSummary.getAuthor(),
                            reviewSummary.getSubject(), reviewSummary.getContent(), null);
                    integration.createReview(review);
                });
            }

            LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());
        } catch (RuntimeException re) {
            LOG.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    /**
     * The composite delete method simply calls the three delete methods in the integration layer to delete
     * the corresponding entities in the underlying databases
     * @param productId
     */
    @Override
    public void deleteProduct(int productId) {

        LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

        integration.deleteProduct(productId);
        integration.deleteReviews(productId);
        integration.deleteRecommendations(productId);

        LOG.debug("deleteCompositeProduct: aggregate entities deleted for productId: {}", productId);
    }

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations,
                                                    List<Review> reviews, String serviceAddress) {

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries =
                (recommendations == null) ? null : recommendations.stream()
                        .map((Recommendation r) -> {
                            return new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent());
                        })
                        .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries =
                (reviews == null) ? null : reviews.stream()
                        .map((Review r) -> {
                            return new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent());
                        })
                        .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
