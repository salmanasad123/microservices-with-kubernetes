package com.microservices.core.review_service.services;

import com.example.api.api.core.review.Review;
import com.example.api.api.core.review.ReviewService;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.util.util.ServiceUtil;
import com.microservices.core.review_service.persistence.ReviewEntity;
import com.microservices.core.review_service.persistence.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;

    private final ReviewRepository reviewRepository;

    private final ReviewMapper reviewMapper;

    //// constructor
    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository reviewRepository,
                             ReviewMapper reviewMapper) {
        this.serviceUtil = serviceUtil;
        this.reviewMapper = reviewMapper;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public List<Review> getReviews(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        List<ReviewEntity> entityList = reviewRepository.findByProductId(productId);
        List<Review> list = reviewMapper.entityListToApiList(entityList);
        list.forEach((Review review) -> {
            review.setServiceAddress(serviceUtil.getServiceAddress());
        });

        LOG.debug("getReviews: response size: {}", list.size());

        return list;
    }

    @Override
    public Review createReview(Review body) {
        try {
            ReviewEntity reviewEntity = reviewMapper.apiToEntity(body);
            ReviewEntity newEntity = reviewRepository.save(reviewEntity);

            LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
            return reviewMapper.entityToApi(newEntity);

        } catch (DuplicateKeyException duplicateKeyException) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()
                    + ", Review Id:" + body.getReviewId());
        }
    }

    @Override
    public void deleteReviews(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        reviewRepository.deleteAll(reviewRepository.findByProductId(productId));
    }
}
