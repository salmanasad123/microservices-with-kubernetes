package com.microservices.core.product_composite_service;

import com.example.api.api.core.product.Product;
import com.example.api.api.core.recommendation.Recommendation;
import com.example.api.api.core.review.Review;
import com.example.api.api.exceptions.InvalidInputException;
import com.example.api.api.exceptions.NotFoundException;
import com.microservices.core.product_composite_service.services.ProductCompositeIntegration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * First, we declare three constants that are used in the test class: PRODUCT_ID_OK, PRODUCT_ID_
 * NOT_FOUND, and PRODUCT_ID_INVALID.
 * Next, the annotation @MockBean is used to configure Mockito to set up a mock for the
 * ProductCompositeIntegration interface.
 * If the getProduct(), getRecommendations(), and getReviews() methods are called on the
 * integration component, and productId is set to PRODUCT_ID_OK, the mock will return a normal
 * response.
 */

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductCompositeServiceApplicationTests {

	private static final int PRODUCT_ID_OK = 1;
	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

	@Autowired
	private WebTestClient client;

	/**
	 * To test the composite product API in isolation, we need to mock its dependencies, that is,
	 * the requests to the other three microservices(product, review & recommendation)
	 * that were performed by the integration component, ProductCompositeIntegration. We use Mockito to do this
	 */
	@MockBean
	private ProductCompositeIntegration compositeIntegration;

	@BeforeEach
	void setUp() {

		when(compositeIntegration.getProduct(PRODUCT_ID_OK)).thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));

		when(compositeIntegration.getRecommendations(PRODUCT_ID_OK)).thenReturn(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));

		when(compositeIntegration.getReviews(PRODUCT_ID_OK)).thenReturn(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));

		when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND)).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

		when(compositeIntegration.getProduct(PRODUCT_ID_INVALID)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
	}

	@Test
	void contextLoads() {}

	@Test
	void getProductById() {

		client.get()
				.uri("/product-composite/" + PRODUCT_ID_OK)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
				.jsonPath("$.recommendations.length()").isEqualTo(1)
				.jsonPath("$.reviews.length()").isEqualTo(1);
	}

	@Test
	void getProductNotFound() {

		client.get()
				.uri("/product-composite/" + PRODUCT_ID_NOT_FOUND)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
				.jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
	}

	@Test
	void getProductInvalidInput() {

		client.get()
				.uri("/product-composite/" + PRODUCT_ID_INVALID)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
				.jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
	}

}
