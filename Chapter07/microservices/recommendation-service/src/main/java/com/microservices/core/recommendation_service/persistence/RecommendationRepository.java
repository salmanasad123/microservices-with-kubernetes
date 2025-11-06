package com.microservices.core.recommendation_service.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Making the MongoDB-based repositories for the product and recommendation services reactive is
 * very simple: Change the base class for the repositories to ReactiveCrudRepository.
 * Change the custom finder methods to return either a Mono or a Flux object.
 */
public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {

    // The findByProductId method will return zero to many recommendation entities, so the
    // return value is defined as a list
    Flux<RecommendationEntity> findByProductId(int productId);
}
