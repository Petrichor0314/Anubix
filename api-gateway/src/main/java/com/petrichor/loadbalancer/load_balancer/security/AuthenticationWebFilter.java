package com.petrichor.loadbalancer.load_balancer.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationWebFilter.class);
    private final JwtValidationUtil jwtValidationUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Define paths that should bypass JWT validation in this filter
    private final List<String> permitAllPaths = Arrays.asList(
        "/auth/auth/login",
        "/api/auth-service/auth/login", // Kept for flexibility
        "/actuator/**",
        "/api-gateway/actuator/**",
        "/auth/actuator/**",
        "/features/actuator/**",
        "/toggles/actuator/**",
        "/analytics/actuator/**"
    );

    
    public AuthenticationWebFilter(JwtValidationUtil jwtValidationUtil) {
        this.jwtValidationUtil = jwtValidationUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        log.debug("[AuthWebFilter] Processing request for path: {}", path);

        // Check if the path should be permitted without JWT validation by this filter
        boolean isPermitted = permitAllPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (isPermitted) {
            log.debug("[AuthWebFilter] Path {} is permitted to bypass JWT validation by this filter.", path);
            return chain.filter(exchange); // Proceed without JWT check
        }

        log.debug("[AuthWebFilter] Path {} requires JWT validation.", path);
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String authToken = authHeader.substring(7);
            if (jwtValidationUtil.validateToken(authToken)) {
                String username = jwtValidationUtil.extractUsername(authToken);
                List<String> roles = jwtValidationUtil.extractRoles(authToken);
                String userId = jwtValidationUtil.extractUserId(authToken);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Name", username)
                    .header("X-User-Roles", String.join(",", roles))
                    .build();
                
                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                log.debug("[AuthWebFilter] Valid JWT. Forwarding with user details for path: {}", path);
                return chain.filter(mutatedExchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            } else {
                 log.warn("[AuthWebFilter] Invalid JWT token for path: {}", path);
            }
        } else {
            log.warn("[AuthWebFilter] Missing or malformed Authorization header for path: {}", path);
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
} 