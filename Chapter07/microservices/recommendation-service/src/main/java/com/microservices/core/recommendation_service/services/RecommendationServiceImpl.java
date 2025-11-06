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

import java.util.ArrayList;
import java.util.List;

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
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        List<RecommendationEntity> entityList = recommendationRepository.findByProductId(productId);
        List<Recommendation> recommendationList = recommendationMapper.entityListToApiList(entityList);

        recommendationList.forEach((Recommendation recommendation) -> {
            recommendation.setServiceAddress(serviceUtil.getServiceAddress());
        });

        LOG.debug("/recommendation response size: {}", recommendationList.size());

        return recommendationList;
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        try {

            RecommendationEntity recommendationEntity = recommendationMapper.apiToEntity(body);
            RecommendationEntity newEntity = recommendationRepository.save(recommendationEntity);
            return recommendationMapper.entityToApi(newEntity);
        } catch (DuplicateKeyException exception) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId());
        }
    }

    // We will be deleting the entire list of recommendations associated with a product.
    @Override
    public void deleteRecommendations(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        recommendationRepository.deleteAll(recommendationRepository.findByProductId(productId));
    }
}
