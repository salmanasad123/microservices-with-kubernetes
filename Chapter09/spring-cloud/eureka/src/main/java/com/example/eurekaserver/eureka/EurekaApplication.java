package com.example.eurekaserver.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud comes with an abstraction of how to communicate with a discovery service such as
 * Netflix Eureka and provides an interface called DiscoveryClient. This can be used to interact with a
 * discovery service to get information regarding available services and instances. Implementations of
 * the DiscoveryClient interface are also capable of automatically registering a Spring Boot application
 * with the discovery server.
 *
 * Spring Boot can find implementations of the DiscoveryClient interface automatically during start
 * up, so we only need to bring in a dependency on the corresponding implementation to connect to a
 * discovery server. In the case of Netflix Eureka, the dependency that’s used by our microservices is
 * spring-cloud-starter-netflix-eureka-client.
 *
 * Suppose product-composite service need to talk with the review-service, and there are 3 instances of review
 * service running, so the product-composite service will ask Eureka service for review service instances,
 * it will get the list of ips, and then it will send request in a round-robin fashion to those 3 ips. This is
 * called client side load-balancing. Product-composite service is the client of Eureka Server.
 *
 * Process:
 *  1. Whenever a microservice instance starts up – for example, the Review service – it registers
 * itself to one of the Eureka servers.
 *  2. On a regular basis, each microservice instance sends a heartbeat message to the Eureka server,
 * telling it that the microservice instance is okay and is ready to receive requests.
 *  3. Clients – for example, the Product Composite service – use a client library that regularly asks
 * the Eureka service for information about available services.
 *  4. When the client needs to send a request to another microservice, it already has a list of available
 * instances in its client library and can pick one of them without asking the discovery server.
 * Typically, available instances are chosen in a round-robin fashion; that is, they are called one
 * after another before the first one is called once more.
 */
@SpringBootApplication
public class EurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaApplication.class, args);
	}

}
