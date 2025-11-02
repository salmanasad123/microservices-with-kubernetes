package com.microservices.core.recommendation_service;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * When writing persistence tests, we want to start a database when the tests begin and tear it down
 * when the tests are complete. However, we don’t want the tests to wait for other resources to start up,
 * for example, a web server such as Netty (which is required at runtime).
 * Spring Boot comes with two class-level annotations tailored to this specific requirement:
 * • @DataMongoTest: This annotation starts up a MongoDB database when the test starts.
 * • @DataJpaTest: This annotation starts up a SQL database when the test starts
 * <p>
 * To handle the startup and tear-down of databases during the execution of the integration tests, we
 * will use Testcontainers
 * <p>
 * Testcontainers is a library that simplifies running automated integration tests by running resource managers
 * like a database or a message broker as a Docker container. Testcontainers can be configured to automatically
 * start up Docker containers when JUnit tests are started and tear down the containers when the tests are complete.
 * <p>
 * To enable Testcontainers in an existing test class for a Spring Boot application like the microservices
 * in this book, we can add the @Testcontainers annotation to the test class. Using the @Container
 * annotation, we can, for example, declare that the Review microservice’s integration tests will use a
 * Docker container running MySQL
 */

public class MongoDbTestBase {

    @Container
    private static MongoDBContainer database =
            new MongoDBContainer("mongo:6.0.4");

    // A static block is used to start the database container before any JUnit code is invoked
    static {
        database.start();
    }

    // The database container will get some properties defined when started up, such as which port to
    // use. To register these dynamically created properties in the application context, a static method
    // databaseProperties() is defined. The method is annotated with @DynamicPropertySource
    // to override the database configuration in the application context, such as the configuration
    // from an application.yml file.

    // Problem: Spring Boot ko ye dynamic (runtime generated) port aur URL kaise pata chale?
    // Jab hum Testcontainers use karte hain, to container ke port, URL, username, password runtime par banaye jaate hain.
    // @DynamicPropertySource use karke hum ye dynamic properties Spring Boot ke application context me inject karte hain
    // taake wo us database se connect kar sake.
    // Ye method static hoti hai aur application.yml ke DB config ko override karti hai.

    // Jab test start hota hai:
    // Testcontainers ek PostgreSQL container start karta hai.
    // Wo random port aur DB name assign karta hai (jaise jdbc:postgresql://localhost:54321/test).
    // @DynamicPropertySource wala method run hota hai before Spring context loads.
    // Ye runtime par Spring ke applicationContext me properties inject karta hai.
    // Yani application.yml ke DB config ko override kar deta hai.
    // Ab aapka Spring Boot test automatically Testcontainers DB se connect kar leta hai.

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host", () -> database.getContainerIpAddress());
        registry.add("spring.data.mongodb.port", () -> database.getMappedPort(27017));
        registry.add("spring.data.mongodb.database", () -> "test");
    }

}
