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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


@RestController
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;

    private final ReviewRepository reviewRepository;

    private final ReviewMapper reviewMapper;

    private Scheduler jdbcScheduler;

    //// constructor
    @Autowired
    public ReviewServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, ServiceUtil serviceUtil,
                             ReviewRepository reviewRepository, ReviewMapper reviewMapper) {
        this.serviceUtil = serviceUtil;
        this.reviewMapper = reviewMapper;
        this.reviewRepository = reviewRepository;
        this.jdbcScheduler = jdbcScheduler;
    }

    /**
     * Important concept hai jab hum Spring WebFlux ke saath non-reactive database (like JDBC) use karte hain.
     * Agar isko direct reactive flow me daal dete, to thread block ho jaata — which is bad for WebFlux as it.
     * will block the Web-Flux event loop thread.
     * <p>
     * Ye ek Mono bana raha hai from a blocking call.
     * fromCallable means:
     * “Wait until subscribed, then call this function in a safe, lazy way.”
     * It does not block immediately, it just defines the pipeline.
     * When subscribed → it will call internalGetReviews().
     * <p>
     * Iske wajah se reactive WebFlux threads free rehte hain, aur blocking kaam alag thread per hota hai
     * (is scheduler ke through)
     *
     * @param productId Id of the product.
     * @return
     */
    @Override
    public Flux<Review> getReviews(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        LOG.info("Will get reviews for product with id={}", productId);

        Flux<Review> reviewFlux = Mono.fromCallable(() -> {
                    return internalGetReviews(productId);
                })
                .flatMapMany((List<Review> reviewList) -> {
                    return Flux.fromIterable(reviewList);
                })
                .log(LOG.getName(), Level.FINE)
                .subscribeOn(jdbcScheduler);

        return reviewFlux;
    }

    /**
     * Reviews ek ek karke asynchronously aate hain, aur system har ek ko process karta hai jaise hi wo
     * milta hai — bina poora list ka wait kiye.
     * <p>
     * Flux<Review> koi “ready-made list” nahi hoti.
     * Ye ek pipeline define karta hai:
     * Jab koi subscribe karega, tab data aana start hoga.
     * Har review ek ek karke emit hoga:
     * onNext(review1)
     * onNext(review2)
     * ...
     * onComplete()
     * So Flux acts like a Publisher that emits events.
     */

    // Here, the blocking code is placed in the internalGetReviews() method and is wrapped
    // in a Mono object using the Mono.fromCallable() method. The getReviews() method uses
    // the subscribeOn() method to run the blocking code in a thread from the thread pool of jdbcScheduler.
    private List<Review> internalGetReviews(int productId) {

        List<ReviewEntity> entityList = reviewRepository.findByProductId(productId);
        List<Review> list = reviewMapper.entityListToApiList(entityList);
        list.forEach((Review review) -> {
            review.setServiceAddress(serviceUtil.getServiceAddress());
        });

        LOG.debug("Response size: {}", list.size());

        return list;
    }

    @Override
    public Mono<Review> createReview(Review body) {

        if (body.getProductId() < 1) {
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        }

        Mono<Review> reviewMono = Mono.fromCallable(() -> {
            return internalCreateReview(body);
        }).subscribeOn(jdbcScheduler);

        return reviewMono;
    }

    private Review internalCreateReview(Review body) {
        try {
            ReviewEntity entity = reviewMapper.apiToEntity(body);
            ReviewEntity newEntity = reviewRepository.save(entity);

            LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
            return reviewMapper.entityToApi(newEntity);

        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
        }
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        return Mono.fromRunnable(() -> {
            internalDeleteReviews(productId);
        }).subscribeOn(jdbcScheduler).then();
    }

    private void internalDeleteReviews(int productId) {

        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);

        reviewRepository.deleteAll(reviewRepository.findByProductId(productId));
    }
}
