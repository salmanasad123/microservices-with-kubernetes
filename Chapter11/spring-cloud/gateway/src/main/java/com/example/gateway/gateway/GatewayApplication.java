package com.example.gateway.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class GatewayApplication {

	/**
	 * The main application class, GatewayApplication, declares a WebClient.Builder bean to be
	 * used by the implementation of the health indicator, as follows:
	 * WebClient.builder is annotated with @LoadBalanced, which makes it aware of microservice instances
	 * registered in the discovery server, Netflix Eureka.
	 */
	@Bean
	@LoadBalanced
	public WebClient.Builder loadBalancedWebClientBuilder() {
		return WebClient.builder();
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
