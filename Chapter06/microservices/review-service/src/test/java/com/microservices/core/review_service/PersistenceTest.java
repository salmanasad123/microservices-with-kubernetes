package com.microservices.core.review_service;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

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
}
