package com.petrichor.loadbalancer.load_balancer.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import com.petrichor.loadbalancer.load_balancer.security.AuthenticationWebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationWebFilter authenticationWebFilter;

    @Autowired
    public SecurityConfig(AuthenticationWebFilter authenticationWebFilter) {
        this.authenticationWebFilter = authenticationWebFilter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        String[] permittedPathsByFilter = {
            "/auth/auth/login", 
            "/api/auth/auth/login",
            "/actuator/**", // Allow direct access to gateway's own actuators
            "/api-gateway/actuator/**", // Keep for consistency
            "/auth/actuator/**",
            "/features/actuator/**",
            "/toggles/actuator/**",
            "/analytics/actuator/**"
        };



        
        String[] permittedPathsForSpringSecurity = permittedPathsByFilter; 

        log.info("[SecurityConfig] Configuring Spring Security. Paths permitted by AuthenticationWebFilter (and for permitAll): {}", Arrays.toString(permittedPathsForSpringSecurity));

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(permittedPathsForSpringSecurity).permitAll() // Use the specific list
                .anyExchange().authenticated()
            )
            .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
} 