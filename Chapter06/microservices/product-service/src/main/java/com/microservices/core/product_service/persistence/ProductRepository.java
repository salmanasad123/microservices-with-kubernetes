package com.microservices.core.product_service.persistence;

import com.example.api.api.core.product.Product;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The CrudRepository interface provides standard methods for performing basic create, read,
 * update, and delete operations on the data stored in the databases.
 * The PagingAndSortingRepository interface adds support for paging and sorting to the
 * CrudRepository interface
 */

@Repository
public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String>,
        CrudRepository<ProductEntity, String> {

    // Spring Data supports defining extra query methods based on naming conventions for the signature
    // of the method. For example, the findByProductId(int productId) method signature can be used to
    // direct Spring Data to automatically create a query that returns entities from the underlying collection
    // or table.
    // Since the findByProductId method might return zero or one product entity, the return value is marked
    // to be optional by wrapping it in an Optional object
    Optional<ProductEntity> findByProductId(int productId);
}
