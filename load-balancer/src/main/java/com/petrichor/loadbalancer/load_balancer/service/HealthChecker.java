package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
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

    public HealthChecker(LoadBalancerConfig config, LoadBalancerService loadBalancerService) {
        this.config = config;
        this.serverInfos = loadBalancerService.getServerInfos();
        this.webClient = WebClient.create();
    }

    @Scheduled(fixedRateString = "${loadbalancer.healthcheck.interval:5000}")
    public void checkHealth() {
        if (!config.isHealthCheckEnabled()) {
            return; // skip health check if disabled
        }

        Flux.fromIterable(serverInfos)
                .flatMap(server -> {
                    String healthUrl = server.getUrl().replace("/api/test", config.getHealthCheckPath());

                    return webClient.get()
                            .uri(healthUrl)
                            .retrieve()
                            .toBodilessEntity()
                            .timeout(Duration.ofSeconds(2))
                            .retryWhen(
                                    Retry.fixedDelay(config.getRetries(), Duration.ofMillis(config.getRetryDelay()))
                                            .filter(e -> !(e instanceof java.util.concurrent.TimeoutException))
                            )
                            .map(response -> {
                                server.setHealthy(true);
                                return server;
                            })
                            .onErrorResume(e -> {
                                server.setHealthy(false);
                                return Mono.just(server);
                            });
                })
                .subscribe(); // triggers execution
    }
}
