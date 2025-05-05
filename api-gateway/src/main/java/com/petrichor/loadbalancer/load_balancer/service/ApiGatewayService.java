package com.petrichor.loadbalancer.load_balancer.service;

import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.algorithm.LoadBalancerAlgorithm;
import com.petrichor.loadbalancer.load_balancer.factory.LoadBalancerAlgorithmFactory;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApiGatewayService {


    private final WebClient webClient;
    private final LoadBalancerAlgorithm loadBalancerAlgorithm;
    private final LoadBalancerConfig config;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final DiscoveryClient discoveryClient;

    public ApiGatewayService(
            LoadBalancerConfig config,
            WebClient webClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            DiscoveryClient discoveryClient
    ) {
        this.webClient = webClient;
        this.config = config;
        this.discoveryClient = discoveryClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.loadBalancerAlgorithm = LoadBalancerAlgorithmFactory.getAlgorithm(config.getAlgorithm());

        System.out.println("[ApiGatewayService] Initialized with load balancer: " +
                this.loadBalancerAlgorithm.getClass().getSimpleName());
    }

    /**
     * Public entrypoint. Forwards the request to a healthy service instance.
     */
    public Mono<String> forwardRequest(String serviceName, String path, String httpMethod) {
        System.out.printf("[forwardRequest] Forwarding %s → service: %s, path: %s%n", httpMethod, serviceName, path);
        return attemptForward(serviceName, path, httpMethod, 0);
    }

    /**
     * Attempts to route the request to a healthy service instance with retries.
     */
    private Mono<String> attemptForward(String serviceName, String path, String method, int attempt) {
        System.out.printf("[attemptForward][Attempt #%d] Attempting to route request to: %s%n", attempt + 1, serviceName);

        List<ServerInfo> healthyInstances = getHealthyInstances(serviceName);

        if (healthyInstances.isEmpty()) {
            System.err.println("[attemptForward] No healthy instances found for service: " + serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No healthy instances for service: " + serviceName));
        }

        Optional<ServerInfo> selected = loadBalancerAlgorithm.selectServer(healthyInstances);

        if (selected.isEmpty()) {
            System.err.println("[attemptForward] Load balancer failed to select an instance for service: " + serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LoadBalancer failed to select instance"));
        }

        ServerInfo server = selected.get();
        server.incrementConnections();

        System.out.printf("[attemptForward]  Routing to instance: %s (%s)%n", server.getUrl(), serviceName);

        return forwardToInstance(server, path, method)
                .doOnError(err -> {
                    System.err.printf("[attemptForward] Request failed to %s: %s%n", server.getUrl(), err.getMessage());
                    server.setHealthy(false);
                })
                .onErrorResume(err -> {
                    System.out.printf("[attemptForward] Retrying after failure on %s%n", server.getUrl());
                    return attemptForward(serviceName, path, method, attempt + 1);
                })
                .doFinally(signalType -> {
                    System.out.printf("[attemptForward] Finished handling request to %s - releasing connection%n", server.getUrl());
                    server.decrementConnections();
                });
    }

    /**
     * Performs the actual HTTP call to the target service instance.
     */
    private Mono<String> forwardToInstance(ServerInfo server, String path, String method) {
        String url = server.getUrl() + path;
        long startTime = System.currentTimeMillis();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(server.getUrl());
        Retry retry = retryRegistry.retry("load-balancer-retry");

        System.out.printf("[forwardToInstance] Sending %s to: %s%n", method, url);

        return webClient.method(HttpMethod.valueOf(method))
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .transform(RetryOperator.of(retry))
                .doOnNext(response -> {
                    double elapsed = System.currentTimeMillis() - startTime;
                    server.setAvgResponseTime((server.getAvgResponseTime() + elapsed) / 2.0);
                    System.out.printf("[forwardToInstance] Received response from %s in %.2f ms%n", server.getUrl(), elapsed);
                });
    }

    /**
     * Retrieves and filters healthy service instances via Eureka.
     */
    private List<ServerInfo> getHealthyInstances(String eurekaServiceName) {
        System.out.printf("[getHealthyInstances] Looking up instances for: %s%n", eurekaServiceName);

        List<ServiceInstance> instances = discoveryClient.getInstances(eurekaServiceName);

        System.out.printf("[getHealthyInstances] Found %d instances for %s%n", instances.size(), eurekaServiceName);

        return instances.stream()
                .map(instance -> new ServerInfo(instance.getUri().toString()))
                .filter(ServerInfo::isHealthy)
                .collect(Collectors.toList());
    }
}