package com.example.eurekaserver.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Now we will use HTTP Basic authentication to restrict access to the APIs and web pages on the discovery server,
 * Netflix Eureka. This means that we will require a user to supply a username and password to get access.
 * Changes are required both on the Eureka server and in the Eureka clients.
 * Eureka server is now secured with Http Basic Authentication
 */
@Configuration
public class SecurityConfig {

    private String username;
    private String password;

    // The username and password are injected into the constructor from the configuration file.
    @Autowired
    public SecurityConfig(@Value("${app.eureka-username}") String username,
                          @Value("${app.eureka-password}") String password) {
        this.username = username;
        this.password = password;
    }

    // The return value from this method will be registered as a bean in spring-context.
    // This creates an in-memory user store which is fine for testing purposes, in production we use
    // LDAP or DB.
    // I have overridden spring default user and defined our own user with password.
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails userDetails = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // This security config is registered with Spring Security. Http basic means requires a username and
    // password.
    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                // Disable CRCF to allow services to register themselves with Eureka
                .csrf((CsrfConfigurer<HttpSecurity> csrf) -> {
                    csrf.disable();
                })
                .authorizeHttpRequests((AuthorizeHttpRequestsConfigurer<HttpSecurity>.
                                                AuthorizationManagerRequestMatcherRegistry auth) -> {
                    auth.anyRequest().authenticated();
                })
                .httpBasic((HttpBasicConfigurer<HttpSecurity> basic) -> {
                });

        return http.build();
    }
}
