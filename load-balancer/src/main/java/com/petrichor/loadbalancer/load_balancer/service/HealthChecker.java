package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient; // Keep this import
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

    public HealthChecker(
            LoadBalancerConfig config,
            LoadBalancerService loadBalancerService,
            WebClient webClient // Add WebClient parameter for injection
    ) {
        this.config = config;
        this.serverInfos = loadBalancerService.getServerInfos();
        this.webClient = webClient;
    }

    @Scheduled(fixedRateString = "${loadbalancer.healthcheck.interval:5000}")
    public void checkHealth() {
        if (!config.isHealthCheckEnabled()) {
            // Consider logging that health check is disabled
            // log.debug("Health check is disabled via configuration.");
            return;
        }

        // log.trace("Starting periodic health check for {} servers.", serverInfos.size());
        Flux.fromIterable(serverInfos)
                .flatMap(server -> {
                    // Construct health check URL safely
                    // Consider potential issues if base URL already has a path
                    String baseUrl = server.getUrl().replace("/api/test", ""); // Basic replacement
                    String healthPath = config.getHealthCheckPath().startsWith("/") ? config.getHealthCheckPath() : "/" + config.getHealthCheckPath();
                    String healthUrl = baseUrl + healthPath;

                    // log.trace("Checking health for server {} at URL: {}", server.getUrl(), healthUrl);

                    return webClient.get() // Use the injected webClient instance
                            .uri(healthUrl)
                            .retrieve()
                            .toBodilessEntity() // Efficiently discard body
                            .timeout(Duration.ofSeconds(config.getHealthCheckTimeoutSeconds())) // Use configured timeout
                            .retryWhen(
                                    Retry.fixedDelay(config.getHealthCheckRetries(), Duration.ofMillis(config.getHealthCheckRetryDelayMillis()))
                                            // Optionally add more sophisticated error filtering for retries
                                            .filter(e -> {
                                                // Example: Log retry attempts
                                                // log.warn("Retrying health check for {} due to error: {}", healthUrl, e.getMessage());
                                                return true; // Retry on most errors for health checks, timeout is handled by .timeout()
                                            })
                            )
                            .doOnSuccess(response -> { // Use doOnSuccess for side-effects on success
                                if (!server.isHealthy()) {
                                    // log.info("Server {} is now healthy.", server.getUrl());
                                }
                                server.setHealthy(true);
                            })
                            .onErrorResume(e -> { // Handle errors after timeout and retries
                                if (server.isHealthy()) {
                                    // log.warn("Server {} marked as unhealthy. Error: {}", server.getUrl(), e.getMessage());
                                } else {
                                    // Optionally log less verbosely if already unhealthy
                                    // log.trace("Server {} remains unhealthy. Error: {}", server.getUrl(), e.getMessage());
                                }
                                server.setHealthy(false);
                                return Mono.empty(); // Return empty Mono to continue the Flux processing after error
                            })
                            // Return the server info regardless of outcome if needed downstream,
                            // otherwise Mono.empty() in onErrorResume is fine.
                            // Using Mono.empty() above means only healthy transitions matter if you collect results.
                            // If you wanted to know about all servers checked, you might return Mono.just(server) here.
                            .then(); // Ensure completion signal for flatMap even on error handled by onErrorResume
                })
                // Add logging for unexpected errors during the Flux processing itself
                .doOnError(fluxError -> System.err.println("Unexpected error during health check flux processing: " + fluxError.getMessage()))
                // Use subscribe with error handling
                .subscribe(
                        null, // No action needed on successful completion of an individual check (handled in doOnSuccess)
                        error -> System.err.println("Error during health check stream execution: " + error.getMessage()) // Log errors in the stream
                        // () -> log.trace("Health check cycle completed.") // Optional: Log completion
                );
    }

    // Consider adding methods to get timeout/retry values from config with defaults
    // Example:
    // private int getHealthCheckTimeoutSeconds() { return config.getHealthCheckTimeoutSeconds() > 0 ? config.getHealthCheckTimeoutSeconds() : 2; }
    // private int getHealthCheckRetries() { return config.getHealthCheckRetries() >= 0 ? config.getHealthCheckRetries() : 1; }
    // private long getHealthCheckRetryDelayMillis() { return config.getHealthCheckRetryDelayMillis() > 0 ? config.getHealthCheckRetryDelayMillis() : 500; }

}