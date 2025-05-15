package com.petrichor.loadbalancer.load_balancer.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ServiceNameResolver {

    private static final Map<String, String> PREFIX_TO_EUREKA = Map.of(
            "features", "FEATURE-SERVICE",
            "toggles", "TOGGLE-SERVICE",
            "analytics", "ANALYTICS-SERVICE"
    );

    /**
     * Resolves a URL prefix (e.g., "features") to its corresponding Eureka service name (e.g., "FEATURE-SERVICE").
     *
     * @param prefix The prefix from the request path.
     * @return Optional containing the resolved service name or empty if unknown.
     */
    public Optional<String> resolveEurekaServiceName(String prefix) {
        return Optional.ofNullable(PREFIX_TO_EUREKA.get(prefix.toLowerCase()));
    }
}
