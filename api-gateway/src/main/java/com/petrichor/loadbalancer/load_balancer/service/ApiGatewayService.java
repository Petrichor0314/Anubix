package com.petrichor.loadbalancer.load_balancer.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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
import reactor.core.publisher.Flux;

@Service
public class ApiGatewayService {
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayService.class);

    private final WebClient webClient;
    private final LoadBalancerAlgorithm loadBalancerAlgorithm;
    private final LoadBalancerConfig config;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final ReactiveDiscoveryClient discoveryClient;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration cacheTtl; // Example: Make this configurable

    public ApiGatewayService(
            LoadBalancerConfig config,
            WebClient webClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            ReactiveDiscoveryClient reactiveDiscoveryClient,
            ReactiveStringRedisTemplate redisTemplate // Inject Redis template
    ) {
        try {
        this.webClient = webClient;
        this.config = config;
            this.discoveryClient = reactiveDiscoveryClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.loadBalancerAlgorithm = LoadBalancerAlgorithmFactory.getAlgorithm(config.getAlgorithm());
            this.redisTemplate = redisTemplate;
            this.cacheTtl = Duration.ofSeconds(config.getCacheTtlSeconds() > 0 ? config.getCacheTtlSeconds() : 60); // Default to 60s, get from config

            logger.info("[ApiGatewayService] Initialized with load balancer: " +
                    this.loadBalancerAlgorithm.getClass().getSimpleName() + " and Cache TTL: " + this.cacheTtl.getSeconds() + "s");
            if (this.redisTemplate == null) {
                logger.error("[ApiGatewayService] CRITICAL: ReactiveStringRedisTemplate is NULL after injection.");
                // Potentially throw an explicit error here if Spring doesn't already, though it should if it's a required dependency
            }
        } catch (Exception e) {
            logger.error("[ApiGatewayService] CRITICAL ERROR in constructor: ", e);
            throw e; // Re-throw to ensure Spring's context failure
        }
    }

    private String generateCacheKey(String serviceName, String path, String httpMethod) {
        // A more robust key might include query parameters or specific headers if they alter the response.
        return String.format("api-gateway-cache:%s:%s:%s", serviceName, path, httpMethod.toUpperCase());
    }

    public Mono<String> forwardRequest(String serviceName, String path, String httpMethod) {
        if (HttpMethod.GET.name().equalsIgnoreCase(httpMethod)) {
            String cacheKey = generateCacheKey(serviceName, path, httpMethod);
            System.out.printf("[forwardRequest] Attempting cache lookup for %s request. Key: %s%n", httpMethod, cacheKey);

            return redisTemplate.opsForValue().get(cacheKey)
                    .flatMap(cachedResponse -> {
                        System.out.printf("[forwardRequest] Cache HIT for key: %s%n", cacheKey);
                        return Mono.just(cachedResponse);
                    })
                    .switchIfEmpty(Mono.<String>defer(() -> {
                        System.out.printf("[forwardRequest] Cache MISS for key: %s. Fetching from service.%n", cacheKey);
                        return resolveAndForward(serviceName, path, httpMethod)
                                .flatMap(responseFromService -> {
                                    // Cache only successful responses (e.g. assuming String response implies success for now)
                                    System.out.printf("[forwardRequest] Caching response for key: %s with TTL: %s%n", cacheKey, cacheTtl);
                                    return redisTemplate.opsForValue().set(cacheKey, responseFromService, cacheTtl)
                                            .thenReturn(responseFromService); // Return the original response after caching
                                });
                    }));
        } else {
            // For non-GET requests, or if caching is disabled for the method, proceed without caching.
            System.out.printf("[forwardRequest] Non-cacheable method %s. Forwarding directly.%n", httpMethod);
            return resolveAndForward(serviceName, path, httpMethod);
        }
    }

    /**
     * Handles the actual resolution of service instance and forwarding the request.
     * This part is called on a cache miss or for non-cacheable methods.
     */
    private Mono<String> resolveAndForward(String serviceName, String path, String httpMethod) {
        logger.info("[resolveAndForward] Processing {} request for service: {}, path: {}", httpMethod, serviceName, path);

        return this.discoveryClient.getInstances(serviceName) // This now returns Flux<ServiceInstance>
            .collectList() // Convert Flux<ServiceInstance> to Mono<List<ServiceInstance>>
            .flatMap(serviceInstances -> {
                if (serviceInstances.isEmpty()) {
                    logger.warn("[resolveAndForward] No instances found for service: {}", serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "No instances available for service: " + serviceName));
                }

                // Map ServiceInstance to ServerInfo. ServerInfo might hold connection counts or response times for advanced LB.
                List<ServerInfo> availableServers = serviceInstances.stream()
                                                                    .map(si -> new ServerInfo(si.getUri().toString()))
                                                                    .toList();

                Optional<ServerInfo> selectedServerOptional = loadBalancerAlgorithm.selectServer(availableServers);

                if (selectedServerOptional.isEmpty()) {
                    logger.warn("[resolveAndForward] Load balancer failed to select an instance for service: {}", serviceName);
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "LoadBalancer failed to select instance for service: " + serviceName));
        }

                ServerInfo server = selectedServerOptional.get();
                
        server.incrementConnections();
                logger.info("[resolveAndForward] Routing to instance: {} for service: {}", server.getUrl(), serviceName); 

                return forwardToInstance(serviceName, server, path, httpMethod)
                .doOnError(err -> {
                            logger.error("[resolveAndForward] Request to {} for service {} failed ultimately: {}", server.getUrl(), serviceName, err.getMessage(), err);
                })
                .doFinally(signalType -> {
                            logger.info("[resolveAndForward] Finished request to {} for service {} - decrementing connections. Signal: {}", server.getUrl(), serviceName, signalType);
                    server.decrementConnections();
                        });
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