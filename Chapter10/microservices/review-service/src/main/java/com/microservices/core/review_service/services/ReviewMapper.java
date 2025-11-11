package com.microservices.core.review_service.services;

import com.example.api.api.core.review.Review;
import com.microservices.core.review_service.persistence.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * MapStruct is used to declare our mapper classes. The use of MapStruct is similar in all three core microservices.
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mappings(@Mapping(target = "serviceAddress", ignore = true))
    Review entityToApi(ReviewEntity reviewEntity);

    @Mappings({@Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)})
    ReviewEntity apiToEntity(Review review);

    List<Review> entityListToApiList(List<ReviewEntity> entity);

    List<ReviewEntity> apiListToEntityList(List<Review> api);
}
