package com.example.gateway.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;


/**
 * With the authorization server in place, we can enhance the edge server and the product-composite
 * service to become OAuth 2.0 resource servers, so that they will require a valid access token to allow
 * access. The edge server will be configured to accept any access token it can validate using the digital
 * signature provided by the authorization server. The product-composite service will also require the
 * access token to contain valid OAuth 2.0 scopes:
 *  • The product:read scope will be required for accessing the read-only APIs.
 *  • The product:write scope will be required for accessing the create and delete APIs.
 *
 *  The @EnableWebFluxSecurity annotation enables Spring Security support for APIs
 *  based on Spring WebFlux
 */


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * We are permitting the routes related to the auth server means they won't require login.
     * Routes to the authorization server for the URIs starting with /oauth, /login, and /error have
     * been added in the configuration file, application.yml. These URIs are used to issue tokens
     * for clients, authenticate users, and show error messages.
     *  • Since these three URIs need to be unprotected by the edge server, they are configured in the
     * new class SecurityConfig to permit all requests.
     *
     * matlab har request jo gateway se pass hoti hai uske paas valid JWT token hona chahiye.
     * oauth2ResourceServer().jwt()
     * Ye gateway ko OAuth2 Resource Server banata hai.
     * Aur .jwt() indicate karta hai ki JWT tokens ko validate kare.
     * Iska matlab ye hua ki: gateway har incoming request me Authorization header me JWT check karega,
     * aur agar token valid nahi hai to 401 Unauthorized return karega.
     */
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws Exception {
        http
                .csrf((ServerHttpSecurity.CsrfSpec csrf) -> {
                    csrf.disable();
                })
                .authorizeExchange((ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchangeSpec) -> {
                    authorizeExchangeSpec.pathMatchers("/headerrouting/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/actuator/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/eureka/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/oauth2/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/login/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/error/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/openapi/**").permitAll();
                    authorizeExchangeSpec.pathMatchers("/webjars/**").permitAll();
                    authorizeExchangeSpec.anyExchange().authenticated();
                })
                // This means that this service is a Resource Server and will accept only valid JWT access tokens.
                // Ye JWT tokens authorization server issue karega.
                //Ye library (spring-security-oauth2-resource-server) un tokens ko verify karegi
                // (signature + expiry + issuer). Verification ke liye ye issuer-uri ya jwk-set-uri config me set
                // hoti hai (e.g., application.yml me).
                // .oauth2ResourceServer().jwt() specifies that authorization will be based on OAuth
                //  2.0 access tokens encoded as JWTs.
                .oauth2ResourceServer((ServerHttpSecurity.OAuth2ResourceServerSpec oauth) -> {
                    oauth.jwt(jwt -> {
                    });
                });

        return http.build();
    }
}