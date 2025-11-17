package com.example.authorizationserver.authorizationserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSecurityConfig.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Setting a security config that will make sure all the requests to this authorization server
     * except the actuator ones will be authenticated, means any end-points exposed by authorization server
     * will require authentication.
     */
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((AuthorizeHttpRequestsConfigurer<HttpSecurity>.
                                            AuthorizationManagerRequestMatcherRegistry authorizeRequests) -> {

                            authorizeRequests
                                    .requestMatchers("/actuator/**").permitAll()
                                    .anyRequest().authenticated();
                        })
                .formLogin(withDefaults());

        return http.build();
    }

    // The username and password for the single registered user are set to u and p, respectively.
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails userDetails = User.builder()
                .username("u")
                .password(passwordEncoder.encode("p"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

    // @formatter:on
}
