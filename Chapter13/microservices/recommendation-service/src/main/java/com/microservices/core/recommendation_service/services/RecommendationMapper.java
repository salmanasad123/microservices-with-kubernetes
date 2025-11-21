package com.microservices.core.recommendation_service.services;

import com.example.api.api.core.recommendation.Recommendation;
import com.microservices.core.recommendation_service.persistence.RecommendationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    // Not only does MapStruct support mapping fields by name but it can also be directed to map fields
    // with different names. In the mapper class for the recommendation service, the rating entity field is
    // mapped to the API model field, rate, using the following annotations:
    @Mappings({
            @Mapping(target = "rate", source = "rating"),
            @Mapping(target = "serviceAddress", ignore = true)})
    Recommendation entityToApi(RecommendationEntity recommendationEntity);

    @Mappings({@Mapping(target = "rating", source = "rate"), @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)})
    RecommendationEntity apiToEntity(Recommendation recommendation);

    List<Recommendation> entityListToApiList(List<RecommendationEntity> entity);

    List<RecommendationEntity> apiListToEntityList(List<Recommendation> api);
}
