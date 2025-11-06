package com.microservices.core.recommendation_service.services;

import com.example.api.api.core.recommendation.Recommendation;
import com.example.api.api.core.recommendation.RecommendationService;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.util.util.ServiceUtil;
import com.microservices.core.recommendation_service.persistence.RecommendationEntity;
import com.microservices.core.recommendation_service.persistence.RecommendationRepository;
import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationService.class);

    private final ServiceUtil serviceUtil;

    private final RecommendationRepository recommendationRepository;

    private final RecommendationMapper recommendationMapper;

    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationRepository recommendationRepository,
                                     RecommendationMapper recommendationMapper) {
        this.serviceUtil = serviceUtil;
        this.recommendationMapper = recommendationMapper;
        this.recommendationRepository = recommendationRepository;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        LOG.info("Will get recommendations for product with id={}", productId);

        Flux<Recommendation> map = recommendationRepository.findByProductId(productId)
                .log(LOG.getName(), Level.FINE)
                .map((RecommendationEntity recommendationEntity) -> {
                    return recommendationMapper.entityToApi(recommendationEntity);
                })
                .map((Recommendation recommendation) -> {
                    recommendation.setServiceAddress(recommendation.getServiceAddress());
                    return recommendation;
                });

        return map;
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {

        if (body.getProductId() < 1) {
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        }

        RecommendationEntity recommendationEntity = recommendationMapper.apiToEntity(body);
        Mono<Recommendation> recommendation = recommendationRepository.save(recommendationEntity)
                .log(LOG.getName(), Level.FINE)
                .onErrorMap(DuplicateKeyException.class, (DuplicateKeyException exception) -> {
                    throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() +
                            ", Recommendation Id:" + body.getRecommendationId());

                })
                .map((RecommendationEntity entity) -> {
                    return recommendationMapper.entityToApi(entity);
                });

            return recommendation;
    }

    // We will be deleting the entire list of recommendations associated with a product.
    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        Mono<Void> voidMono = recommendationRepository.deleteAll(recommendationRepository.findByProductId(productId));
        return voidMono;
    }
}
