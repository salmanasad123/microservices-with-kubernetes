package com.microservices.core.product_service;

import com.microservices.core.product_service.persistence.ProductEntity;
import com.microservices.core.product_service.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.data.domain.Sort.Direction.ASC;

// When using the @DataMongoTest and @DataJpaTest annotations instead of the @SpringBootTest
// annotation to only start up the MongoDB and SQL database during the integration test.
@DataMongoTest
public class PersistenceTest extends MongoDbTestBase {

    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    // The test class, PersistenceTests, declares a method, setupDb(), annotated with @BeforeEach, which
    // is executed before each test method. The setup method removes any entities from previous tests in
    // the database and inserts an entity that the test methods can use as the base for their tests.

    // Since our persistence methods now return a Mono or Flux object, the test methods have to wait for the
    // response to be available in the returned reactive objects. The test methods can either use an explicit call
    // to the block() method on the Mono/Flux object to wait until a response is available, or they can use the StepVerifier
    // helper class from Project Reactor to declare a verifiable sequence of asynchronous events.
    @BeforeEach
    void setupDb() {
        repository.deleteAll();
        ProductEntity entity = new ProductEntity(1, "n", 1);

        // We can use the block() method on the Mono object returned by the repository.findById() method
        // and keep the imperative programming style.
        // Alternatively, we can use the StepVerifier class to set up a sequence of processing steps that both
        // executes the repository find operation and also verifies the result. The sequence is initialized by the
        // final call to the verifyComplete() method like this.
        // Created entity is the object that was returned after saving into the database.
        /**
         * Jab tumne repository.save(entity) likha, to DB operation ka ek publisher bana (yaani ek Mono<ProductEntity>).
         * Abhi tak DB query execute nahi hui — sirf declare hui hai.
         *
         * Reactive world mein “nothing happens until someone subscribes.”
         */
        StepVerifier.create(repository.save(entity))
                .expectNextMatches((ProductEntity createdEntity) -> {
                    savedEntity = createdEntity;
                    return areProductEqual(entity, savedEntity);
                })
                .verifyComplete();
    }

    /**
     * This test creates a new entity, verifies that it can be found using the findById method, and wraps up
     * by asserting that there are two entities stored in the database, the one created by the setup method
     * and the one created by the test itself.
     */
    @Test
    void create() {

        ProductEntity newEntity = new ProductEntity(2, "n", 2);

        StepVerifier.create(repository.save(newEntity))
                .expectNextMatches((ProductEntity createdEntity) -> {
                    return newEntity.getProductId() == createdEntity.getProductId();
                }).verifyComplete();

        StepVerifier.create(repository.findById(newEntity.getId()))
                .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
                .verifyComplete();

        StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
    }

    /**
     * This test updates the entity created by the setup method, reads it again from the database using the
     * standard findById() method, and asserts that it contains expected values for some of its fields. Note
     * that, when an entity is created, its version field is set to 0 by Spring Data, so we expect it to be 1 after
     * the update.
     */
    @Test
    void update() {
        /**
         * Step verifier is a subscriber which attaches itself to the event emitted by MongoDB, when the
         * entity is deleted or saved mongoDb being a reactiveRepository emits an event and put it inside the
         * event-loop, which is then picked and processed by StepVerifier, it attaches ExpectNext method
         * on the event and processes it. When StepVerifier receives onNext() event it triggers its lambda.
         */
        savedEntity.setName("n2");
        StepVerifier.create(repository.save(savedEntity))
                .expectNextMatches((ProductEntity updatedEntity) -> {
                    return updatedEntity.getName().equals("n2");
                })
                .verifyComplete();

        StepVerifier.create(repository.findById(savedEntity.getId()))
                .expectNextMatches((ProductEntity foundEntity) -> {
                    return foundEntity.getVersion() == 1 && foundEntity.getName().equals("n2");
                })
                .verifyComplete();
    }

    /**
     * This test deletes the entity created by the setup method and verifies that it no longer exists in the
     * database.
     */
    @Test
    void delete() {
        // Step verifier is a subscriber which attaches itself to the event emitted by MongoDB, when the
        // entity is deleted or saved mongoDb being a reactiveRepository emits an event and put it inside the
        // event-loop, which is then picked and processed by StepVerifier, it attaches ExpectNext method
        // on the event and processes it. When StepVerifier receives onNext() event it triggers its lambda.
        StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
        StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
    }

    /**
     * This test uses the findByProductId() method to get the entity created by the setup method, verifies
     * that it was found, and then uses the local helper method, assertEqualsProduct(), to verify that the
     * entity returned by findByProductId() looks the same as the entity stored by the setup method.
     */
    @Test
    void getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
                .expectNextMatches((ProductEntity foundEntity) -> {
                    return areProductEqual(savedEntity, foundEntity);
                })
                .verifyComplete();
    }

    /**
     * Next are two test methods that verify alternative flows—handling error conditions. First is a test that
     * verifies that duplicates are handled correctly.
     * The test tries to store an entity with the same business key as used by the entity created by the setup
     * method. The test will fail if the save operation succeeds, or if the save fails with an exception other
     * than the expected DuplicateKeyException
     */
    @Test
    void duplicateError() {
        ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
        StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
    }

    /**
     * The other negative test is, in my opinion, the most interesting test in the test class. It is a test that
     * verifies correct error handling in the case of updates of stale data—it verifies that the optimistic locking
     * mechanism works.
     * <p>
     * The following is observed from the code:
     * • First, the test reads the same entity twice and stores it in two different variables, entity1 and
     * entity2.
     * • Next, it uses one of the variables, entity1, to update the entity. The update of the entity in the
     * database will cause the version field of the entity to be increased automatically by Spring Data.
     * The other variable, entity2, now contains stale data, manifested by its version field, which
     * holds a lower value than the corresponding value in the database.
     * • When the test tries to update the entity using the variable entity2, which contains stale data,
     * it is expected to fail by throwing an OptimisticLockingFailureException exception.
     * • The test wraps up by asserting that the entity in the database reflects the first update, that is,
     * contains the name "n1", and that the version field has the value 1; only one update has been
     * performed on the entity in the database.
     */
    @Test
    void optimisticLockError() {

        // Store the saved entity in two separate entity objects
        ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
        ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

        // Update the entity using the first entity object, If I dont use block() which is a subscriber the
        // save query will not be executed.
        entity1.setName("n1");
        repository.save(entity1).block();

        // Update the entity using the second entity object.
        // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
        StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

        // Get the updated entity from the database and verify its new sate
        StepVerifier.create(repository.findById(savedEntity.getId()))
                .expectNextMatches((ProductEntity foundEntity) -> {
                    return foundEntity.getVersion() == 1 && foundEntity.getName().equals("n1");
                })
                .verifyComplete();
    }

//    @Test
//    void paging() {
//
//        repository.deleteAll();
//
//        List<ProductEntity> newProducts = rangeClosed(1001, 1010)
//                .mapToObj(i -> new ProductEntity(i, "name " + i, i))
//                .collect(Collectors.toList());
//        repository.saveAll(newProducts);
//
//        Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
//        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
//        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
//        nextPage = testNextPage(nextPage, "[1009, 1010]", false);
//    }

//    private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
//        Page<ProductEntity> productPage = repository.findAll(nextPage);
//        assertEquals(expectedProductIds, productPage.getContent().stream().map(p -> p.getProductId()).collect(Collectors.toList()).toString());
//        assertEquals(expectsNextPage, productPage.hasNext());
//        return productPage.nextPageable();
//    }

    private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
        assertEquals(expectedEntity.getName(), actualEntity.getName());
        assertEquals(expectedEntity.getWeight(), actualEntity.getWeight());
    }

    private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
        return
                (expectedEntity.getId().equals(actualEntity.getId()))
                        && (expectedEntity.getVersion() == actualEntity.getVersion())
                        && (expectedEntity.getProductId() == actualEntity.getProductId())
                        && (expectedEntity.getName().equals(actualEntity.getName()))
                        && (expectedEntity.getWeight() == actualEntity.getWeight());
    }
}
