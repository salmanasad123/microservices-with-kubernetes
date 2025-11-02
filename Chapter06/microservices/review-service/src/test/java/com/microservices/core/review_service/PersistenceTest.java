package com.microservices.core.review_service;

import com.microservices.core.review_service.persistence.ReviewEntity;
import com.microservices.core.review_service.persistence.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * Finally, when using the @DataMongoTest and @DataJpaTest annotations instead of the @SpringBootTest
 * annotation to only start up the MongoDB and SQL database during the integration test, there is one
 * more thing to consider. The @DataJpaTest annotation is designed to start an embedded database by
 * default. Since we want to use a containerized database, we have to disable this feature.
 * For the @DataJpaTest annotation, this can be done by using an @AutoConfigureTestDatabase annotation.
 */

@DataJpaTest
@Transactional(propagation = NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PersistenceTest extends MySqlTestBase {

    @Autowired
    private ReviewRepository repository;

    private ReviewEntity savedEntity;

    // The test class, PersistenceTests, declares a method, setupDb(), annotated with @BeforeEach, which
    // is executed before each test method. The setup method removes any entities from previous tests in
    // the database and inserts an entity that the test methods can use as the base for their tests.
    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
        savedEntity = repository.save(entity);

        assertEqualsReview(entity, savedEntity);
    }


    /**
     * This test creates a new entity, verifies that it can be found using the findById method, and wraps up
     * by asserting that there are two entities stored in the database, the one created by the setup method
     * and the one created by the test itself.
     */
    @Test
    void create() {

        ReviewEntity newEntity = new ReviewEntity(1, 3, "a", "s", "c");
        repository.save(newEntity);

        ReviewEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsReview(newEntity, foundEntity);

        assertEquals(2, repository.count());
    }

    /**
     * This test updates the entity created by the setup method, reads it again from the database using the
     * standard findById() method, and asserts that it contains expected values for some of its fields. Note
     * that, when an entity is created, its version field is set to 0 by Spring Data, so we expect it to be 1 after
     * the update.
     */
    @Test
    void update() {
        savedEntity.setAuthor("a2");
        repository.save(savedEntity);

        ReviewEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (long) foundEntity.getVersion());
        assertEquals("a2", foundEntity.getAuthor());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    /**
     * This test uses the findByProductId() method to get the entity created by the setup method, verifies
     * that it was found, and then uses the local helper method, assertEqualsProduct(), to verify that the
     * entity returned by findByProductId() looks the same as the entity stored by the setup method.
     */
    @Test
    void getByProductId() {
        List<ReviewEntity> entityList = repository.findByProductId(savedEntity.getProductId());

        assertThat(entityList, hasSize(1));
        assertEqualsReview(savedEntity, entityList.get(0));
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
        assertThrows(DataIntegrityViolationException.class, () -> {
            ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
            repository.save(entity);
        });

    }

    /**
     * The other negative test is, in my opinion, the most interesting test in the test class. It is a test that
     * verifies correct error handling in the case of updates of stale data—it verifies that the optimistic locking
     * mechanism works.
     *
     * The following is observed from the code:
     *  • First, the test reads the same entity twice and stores it in two different variables, entity1 and
     *    entity2.
     *  • Next, it uses one of the variables, entity1, to update the entity. The update of the entity in the
     *    database will cause the version field of the entity to be increased automatically by Spring Data.
     *    The other variable, entity2, now contains stale data, manifested by its version field, which
     *    holds a lower value than the corresponding value in the database.
     *  • When the test tries to update the entity using the variable entity2, which contains stale data,
     *    it is expected to fail by throwing an OptimisticLockingFailureException exception.
     *  • The test wraps up by asserting that the entity in the database reflects the first update, that is,
     * contains the name "n1", and that the version field has the value 1; only one update has been
     * performed on the entity in the database.
     */

    @Test
    void optimisticLockError() {

        // Store the saved entity in two separate entity objects
        ReviewEntity entity1 = repository.findById(savedEntity.getId()).get();
        ReviewEntity entity2 = repository.findById(savedEntity.getId()).get();

        // Update the entity using the first entity object
        entity1.setAuthor("a1");
        repository.save(entity1);

        // Update the entity using the second entity object.
        // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setAuthor("a2");
            repository.save(entity2);
        });

        // Get the updated entity from the database and verify its new sate
        ReviewEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (int) updatedEntity.getVersion());
        assertEquals("a1", updatedEntity.getAuthor());
    }

    private void assertEqualsReview(ReviewEntity expectedEntity, ReviewEntity actualEntity) {
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
        assertEquals(expectedEntity.getReviewId(), actualEntity.getReviewId());
        assertEquals(expectedEntity.getAuthor(), actualEntity.getAuthor());
        assertEquals(expectedEntity.getSubject(), actualEntity.getSubject());
        assertEquals(expectedEntity.getContent(), actualEntity.getContent());
    }
}
