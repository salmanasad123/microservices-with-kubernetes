package com.example.config.configserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                // Disable CRCF to allow POST to /encrypt and /decrypt endpoins
                .csrf((CsrfConfigurer<HttpSecurity> csrf) -> {
                    csrf.disable();
                })
                .authorizeHttpRequests((AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) -> {
                    auth.anyRequest().authenticated();
                })
                .httpBasic((HttpBasicConfigurer<HttpSecurity> httpSecurityHttpBasicConfigurer) ->  {

                });

        return http.build();
    }
}
