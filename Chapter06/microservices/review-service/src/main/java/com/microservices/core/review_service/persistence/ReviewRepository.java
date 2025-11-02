package com.microservices.core.review_service.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewRepository extends CrudRepository<ReviewEntity, Integer> {

    // Since SQL databases are transactional, we have to specify the default transaction type—read-only in
    // our case—for the query method, findByProductId(). Every query runs in a transaction.
    // Transaction means a basic block of operation that will either run completely or fails, if any error occurs
    // the transaction is rolled back.
    // This query will only read the data it won't update or delete. So keep the query fast.
    @Transactional(readOnly = true)
    List<ReviewEntity> findByProductId(int productId);
}
