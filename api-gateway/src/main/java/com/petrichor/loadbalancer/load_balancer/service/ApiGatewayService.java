package com.petrichor.loadbalancer.load_balancer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.petrichor.loadbalancer.load_balancer.algorithm.LoadBalancerAlgorithm;
import com.petrichor.loadbalancer.load_balancer.config.LoadBalancerConfig;
import com.petrichor.loadbalancer.load_balancer.factory.LoadBalancerAlgorithmFactory;
import com.petrichor.loadbalancer.load_balancer.model.ServerInfo;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import reactor.core.publisher.Mono;

@Service
public class ApiGatewayService {


    private final WebClient webClient;
    private final LoadBalancerAlgorithm loadBalancerAlgorithm;
    private final LoadBalancerConfig config;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final DiscoveryClient discoveryClient;
    private final RateLimiterRegistry rateLimiterRegistry;

    public ApiGatewayService(
            LoadBalancerConfig config,
            WebClient webClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            DiscoveryClient discoveryClient
    ) {
        this.webClient = webClient;
        this.config = config;
        this.discoveryClient = discoveryClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.loadBalancerAlgorithm = LoadBalancerAlgorithmFactory.getAlgorithm(config.getAlgorithm());

        System.out.println("[ApiGatewayService] Initialized with load balancer: " +
                this.loadBalancerAlgorithm.getClass().getSimpleName());
    }


    /**
     * Public entrypoint. Forwards the request to a service instance chosen by the load balancer.
     * Resilience4j policies (Retry, CircuitBreaker, RateLimiter) are applied to the call on the selected instance.
     */
    public Mono<String> forwardRequest(String serviceName, String path, String httpMethod) {
        System.out.printf("[forwardRequest] Processing %s request for service: %s, path: %s%n", httpMethod, serviceName, path);

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);

        if (serviceInstances.isEmpty()) {
            System.err.println("[forwardRequest] No instances found for service: " + serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No instances available for service: " + serviceName));
        }

        List<ServerInfo> availableServers = serviceInstances.stream()
                                                            .map(si -> new ServerInfo(si.getUri().toString())) // Assuming ServerInfo constructor takes URI
                                                            .toList();

        Optional<ServerInfo> selectedServerOptional = loadBalancerAlgorithm.selectServer(availableServers);

        if (selectedServerOptional.isEmpty()) {
            System.err.println("[forwardRequest] Load balancer failed to select an instance for service: " + serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LoadBalancer failed to select instance for service: " + serviceName));
        }

        ServerInfo server = selectedServerOptional.get();
        
        server.incrementConnections(); 
        System.out.printf("[forwardRequest] Routing to instance: %s (%s)%n", server.getUrl(), serviceName);

        return forwardToInstance(serviceName, server, path, httpMethod)
                .doOnError(err -> {
                    System.err.printf("[forwardRequest] Request to %s failed ultimately: %s%n", server.getUrl(), err.getMessage());
                
                })
                .doFinally(signalType -> {
                    System.out.printf("[forwardRequest] Finished request to %s - decrementing connections%n", server.getUrl());
                    server.decrementConnections();
                });
    }

    /**
     * Performs the actual HTTP call to the target service instance,
     * wrapped with Resilience4j policies (RateLimiter, CircuitBreaker, Retry).
     */
    private Mono<String> forwardToInstance(String serviceName, ServerInfo server, String path, String method)
    {
        String url = server.getUrl() + path;
        long startTime = System.currentTimeMillis(); // For measuring response time, potentially for ServerInfo

        // Fetch service-specific or global Resilience4j components
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName); 
        Retry retry = retryRegistry.retry("load-balancer-retry"); 
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(serviceName); // Service-specific rate limiter

        System.out.printf("[forwardToInstance] Sending %s to: %s%n", method, url);

        return webClient.method(HttpMethod.valueOf(method))
                .uri(url)
                .retrieve() // Simple retrieve, assumes response body is String and handles errors via status codes
                .bodyToMono(String.class)
                .transform(RateLimiterOperator.of(rateLimiter))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .transform(RetryOperator.of(retry))
                .doOnSuccess(response -> { 
                    double elapsed = System.currentTimeMillis() - startTime;
                    server.setAvgResponseTime((server.getAvgResponseTime() + elapsed) / 2.0); 
                    System.out.printf("[forwardToInstance] Received response from %s in %.2f ms%n", server.getUrl(), elapsed);
                });
    }
}