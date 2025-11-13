package com.microservices.core.product_composite_service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.http.HttpMethod.*;

/**
 * With the authorization server in place, we can enhance the edge server and the product-composite
 * service to become OAuth 2.0 resource servers, so that they will require a valid access token to allow
 * access. The edge server will be configured to accept any access token it can validate using the digital
 * signature provided by the authorization server. The product-composite service will also require the
 * access token to contain valid OAuth 2.0 scopes:
 *  • The product:read scope will be required for accessing the read-only APIs.
 *  • The product:write scope will be required for accessing the create and delete APIs.
 *
 * The @EnableWebFluxSecurity annotation enables Spring Security support for APIs
 * based on Spring WebFlux
 */


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Yahan tu OAuth2 scopes ke basis pe authorization kar raha hai:
     * Agar koi GET kare → uske token me "SCOPE_product:read" hona chahiye.
     * Agar koi POST/DELETE kare → "SCOPE_product:write" hona chahiye.
     * Note: Spring automatically prefix karta hai "SCOPE_" jab JWT me "scope": "product:read product:write" jaise
     * claim hota hai.
     */
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange((ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchangeSpec) -> {
                    authorizeExchangeSpec.pathMatchers("/openapi/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/webjars/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/actuator/**").permitAll();
                    authorizeExchangeSpec.pathMatchers(POST, "/product-composite/**").hasAuthority("SCOPE_product:write");
                    authorizeExchangeSpec.pathMatchers(DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write");
                    authorizeExchangeSpec.pathMatchers(GET, "/product-composite/**").hasAuthority("SCOPE_product:read");
                    authorizeExchangeSpec.anyExchange().authenticated();
                })
                // This means that this service is a Resource Server and will accept only valid JWT access tokens.
                // Ye JWT tokens authorization server issue karega.
                // Ye library (spring-security-oauth2-resource-server) un tokens ko verify karegi
                // (signature + expiry + issuer). Verification ke liye ye issuer-uri ya jwk-set-uri config me set
                // hoti hai (e.g., application.yml me).
                // .oauth2ResourceServer().jwt() specifies that authorization will be based on OAuth
                // 2.0 access tokens encoded as JWTs.
                .oauth2ResourceServer((ServerHttpSecurity.OAuth2ResourceServerSpec oAuth2ResourceServerSpec) -> {
                    oAuth2ResourceServerSpec.jwt(jwt -> {
                    });
                });

        return http.build();
    }
}
