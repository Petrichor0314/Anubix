package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.factory.LoadBalancerAlgorithmFactory;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import com.petrichor.loadbalancer.load_balancer.algorithm.LoadBalancerAlgorithm;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class LoadBalancerService {

    private final List<ServerInfo> serverInfos;
    private final WebClient webClient;
    private final LoadBalancerAlgorithm loadBalancerAlgorithm;
    private final LoadBalancerConfig config;

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public LoadBalancerService(LoadBalancerConfig config, WebClient webClient, CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.webClient = webClient;
        this.config = config;
        this.serverInfos = new CopyOnWriteArrayList<>();

        // Manually configured backend servers
        serverInfos.add(new ServerInfo("http://backend-server-1:8080/api/test"));
        serverInfos.add(new ServerInfo("http://backend-server-2:8080/api/test"));
        serverInfos.add(new ServerInfo("http://backend-server-3:8080/api/test"));

        this.loadBalancerAlgorithm = LoadBalancerAlgorithmFactory.getAlgorithm(config.getAlgorithm());

        // Initialize circuit breakers for each server
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    public List<ServerInfo> getServerInfos() {
        return serverInfos;
    }

    public Optional<ServerInfo> selectServer(List<ServerInfo> candidates) {
        return loadBalancerAlgorithm.selectServer(candidates);
    }

    public Mono<String> forwardRequest() {
        return attemptForward(0);
    }

    private Mono<String> attemptForward(int attempt) {
        if (attempt >= config.getRetries()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "All retries failed. No backend responded."));
        }

        List<ServerInfo> healthyServers = serverInfos.stream()
                .filter(ServerInfo::isHealthy)
                .collect(Collectors.toList());

        if (healthyServers.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No healthy servers available"));
        }

        Optional<ServerInfo> optionalServer = selectServer(healthyServers);
        if (optionalServer.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No server could be selected"));
        }

        ServerInfo server = optionalServer.get();
        server.incrementConnections(); // still needed for connection tracking

        return forwardRequestToServer(server)
                .doOnError(error -> {
                    server.setHealthy(false);
                    System.err.println("Failed to reach " + server.getUrl() + ": " + error.getMessage());
                })
                .onErrorResume(error -> attemptForward(attempt + 1))
                .doFinally(signal -> server.decrementConnections());
    }


    private Mono<String> forwardRequestToServer(ServerInfo server) {
        long start = System.currentTimeMillis();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(server.getUrl());
        Retry retry = retryRegistry.retry("load-balancer-retry");

        return webClient.get()
                .uri(server.getUrl())
                .retrieve()
                .bodyToMono(String.class)
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .transform(RetryOperator.of(retry))
                .doOnNext(response -> {
                    long elapsed = System.currentTimeMillis() - start;
                    double currentAvg = server.getAvgResponseTime();
                    double newAvg = (currentAvg + elapsed) / 2.0;
                    server.setAvgResponseTime(newAvg);
                });
    }

}
