package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class HealthChecker {

    private final List<ServerInfo> serverInfos;
    private final WebClient webClient;
    private final LoadBalancerConfig config;
    private final CircuitBreaker circuitBreaker;

    public HealthChecker(
            LoadBalancerConfig config,
            LoadBalancerService loadBalancerService,
            WebClient webClient,
            CircuitBreakerRegistry circuitBreakerRegistry // Injected registry
    ) {
        this.config = config;
        this.serverInfos = loadBalancerService.getServerInfos();
        this.webClient = webClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("healthCheckCB");
        this.circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        System.out.println("⚠️ Circuit breaker state changed: " + event.getStateTransition()));
    }

    @Scheduled(fixedRateString = "${loadbalancer.healthcheck.interval:5000}")
    public void checkHealth() {
        if (!config.isHealthCheckEnabled()) return;

        Flux.fromIterable(serverInfos)
                .flatMap(server -> {
                    String baseUrl = server.getUrl().replace("/api/test", "");
                    String healthPath = config.getHealthCheckPath().startsWith("/") ? config.getHealthCheckPath() : "/" + config.getHealthCheckPath();
                    String healthUrl = baseUrl + healthPath;

                    return webClient.get()
                            .uri(healthUrl)
                            .retrieve()
                            .toBodilessEntity()
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .timeout(Duration.ofSeconds(getHealthCheckTimeoutSeconds()))
                            .retryWhen(
                                    Retry.fixedDelay(getHealthCheckRetries(), Duration.ofMillis(getHealthCheckRetryDelayMillis()))
                                            .filter(error -> !(error instanceof IllegalArgumentException
                                                    || error instanceof org.springframework.web.reactive.function.client.WebClientRequestException
                                                    || error instanceof org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest))
                            )
                            .doOnSuccess(response -> {
                                if (!server.isHealthy()) {
                                    System.out.println("✅ Server " + server.getUrl() + " is now healthy.");
                                }
                                server.setHealthy(true);
                            })
                            .onErrorResume(e -> {
                                if (server.isHealthy()) {
                                    System.out.println("❌ Server " + server.getUrl() + " marked unhealthy. Reason: " + e.getClass().getSimpleName());
                                }
                                server.setHealthy(false);
                                return Mono.empty();
                            })
                            .then();
                })
                .subscribe(
                        null,
                        error -> System.err.println("Health check stream error: " + error.getMessage())
                );
    }

    private int getHealthCheckTimeoutSeconds() {
        return config.getHealthCheckTimeoutSeconds() > 0 ? config.getHealthCheckTimeoutSeconds() : 2;
    }

    private int getHealthCheckRetries() {
        return config.getHealthCheckRetries() >= 0 ? config.getHealthCheckRetries() : 1;
    }

    private long getHealthCheckRetryDelayMillis() {
        return config.getHealthCheckRetryDelayMillis() > 0 ? config.getHealthCheckRetryDelayMillis() : 500;
    }
}
