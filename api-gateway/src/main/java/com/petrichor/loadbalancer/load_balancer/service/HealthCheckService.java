package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.HealthCheckConfig;
import com.petrichor.loadbalancer.load_balancer.registry.ServerInfoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class HealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    private final ServerInfoRegistry serverInfoRegistry;
    private final ReactiveDiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final HealthCheckConfig healthCheckConfig;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For parsing health response

    public HealthCheckService(ServerInfoRegistry serverInfoRegistry,
                              ReactiveDiscoveryClient discoveryClient,
                              WebClient.Builder webClientBuilder, // Use WebClient.Builder to create a client for health checks
                              HealthCheckConfig healthCheckConfig) {
        this.serverInfoRegistry = serverInfoRegistry;
        this.discoveryClient = discoveryClient;
        this.webClient = webClientBuilder.build(); // Build a WebClient instance
        this.healthCheckConfig = healthCheckConfig;
    }

    @Scheduled(fixedRateString = "${HEALTHCHECK_INTERVAL_MS:10000}", initialDelayString = "${HEALTHCHECK_INITIAL_DELAY_MS:5000}")
    public void performHealthChecks() {
        if (!healthCheckConfig.isEnabled()) {
            logger.trace("Scheduled health checks are disabled.");
            return;
        }

        logger.debug("Performing scheduled health checks...");
        discoveryClient.getServices()
            .flatMap(serviceId -> {
                // Avoid health checking the discovery-server itself or the api-gateway if listed
                if ("discovery-server".equalsIgnoreCase(serviceId) || 
                    "api-gateway".equalsIgnoreCase(serviceId) || 
                    serviceId.toLowerCase().contains("eureka")) { // General catch for eureka services
                    return Flux.empty();
                }
                return discoveryClient.getInstances(serviceId);
            })
            .flatMap(this::checkHealth)
            .doOnError(error -> logger.error("[HealthCheck] Error during health check processing: {}", error.getMessage(), error))
            .subscribe(
                result -> logger.trace("[HealthCheck] Processed health check for: {} - Healthy: {}", result.serverUrl, result.isHealthy),
                error -> logger.error("[HealthCheck] Unexpected error after health check stream: {}", error.getMessage(), error),
                () -> logger.debug("Finished performing scheduled health checks.")
            );
    }

    private Mono<HealthCheckResult> checkHealth(ServiceInstance instance) {
        String serverUrl = instance.getUri().toString();
        String healthCheckUrl = serverUrl + healthCheckConfig.getPath();
        logger.trace("[HealthCheck] Checking health for {} at {}", instance.getServiceId(), healthCheckUrl);

        return webClient.get()
            .uri(healthCheckUrl)
            .retrieve()
            .bodyToMono(String.class) // Get body as string first
            .flatMap(body -> {
                try {
                    Map<String, Object> healthStatus = objectMapper.readValue(body, new TypeReference<Map<String, Object>>(){});
                    boolean isUp = "UP".equalsIgnoreCase(String.valueOf(healthStatus.get("status")));
                    serverInfoRegistry.updateServerHealth(serverUrl, isUp);
                    return Mono.just(new HealthCheckResult(serverUrl, isUp, "Successfully checked"));
                } catch (Exception e) {
                    logger.warn("[HealthCheck] Failed to parse health response from {}: {}. Marking as unhealthy.", healthCheckUrl, e.getMessage());
                    serverInfoRegistry.updateServerHealth(serverUrl, false);
                    return Mono.just(new HealthCheckResult(serverUrl, false, "Failed to parse response: " + e.getMessage()));
                }
            })
            .timeout(Duration.ofMillis(healthCheckConfig.getTimeoutMs()))
            .onErrorResume(error -> {
                logger.warn("[HealthCheck] Health check failed for {}: {}. Marking as unhealthy.", healthCheckUrl, error.getMessage());
                serverInfoRegistry.updateServerHealth(serverUrl, false);
                return Mono.just(new HealthCheckResult(serverUrl, false, error.getMessage()));
            });
    }

    // Helper class for result logging
    private static class HealthCheckResult {
        final String serverUrl;
        final boolean isHealthy;
        final String details;

        HealthCheckResult(String serverUrl, boolean isHealthy, String details) {
            this.serverUrl = serverUrl;
            this.isHealthy = isHealthy;
            this.details = details;
        }
    }
} 