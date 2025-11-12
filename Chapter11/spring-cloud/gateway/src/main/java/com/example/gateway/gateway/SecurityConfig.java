package com.example.gateway.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

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
                .oauth2ResourceServer((ServerHttpSecurity.OAuth2ResourceServerSpec oauth) -> {
                    oauth.jwt(jwt -> {
                    });
                });

        return http.build();
    }
}