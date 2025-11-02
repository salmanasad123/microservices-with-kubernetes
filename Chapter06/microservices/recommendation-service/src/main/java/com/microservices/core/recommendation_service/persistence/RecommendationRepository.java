package com.microservices.core.recommendation_service.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RecommendationRepository extends CrudRepository<RecommendationEntity, String> {

    // The findByProductId method will return zero to many recommendation entities, so the
    // return value is defined as a list
    List<RecommendationEntity> findByProductId(int productId);
}
