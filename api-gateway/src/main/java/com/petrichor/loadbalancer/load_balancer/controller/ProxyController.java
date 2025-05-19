package com.petrichor.loadbalancer.load_balancer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.petrichor.loadbalancer.load_balancer.service.ApiGatewayService;
import com.petrichor.loadbalancer.load_balancer.util.ServiceNameResolver;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
public class ProxyController {
    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final ApiGatewayService apiGateway;
    private final ServiceNameResolver resolver;

    public ProxyController(ApiGatewayService apiGateway, ServiceNameResolver resolver) {
        this.apiGateway = apiGateway;
        this.resolver = resolver;
    }

    @RequestMapping("/**")
    public Mono<String> routeRequest(ServerHttpRequest request) {
        String rawPath = request.getPath().pathWithinApplication().value(); // e.g. /features/hello
        log.info("[ProxyController] Received raw path: {}", rawPath);

        // Extract headers and body from the incoming request
        HttpHeaders originalHeaders = request.getHeaders();
        Flux<DataBuffer> requestBody = request.getBody();

        String[] segments = rawPath.split("/");

        if (segments.length < 2 || segments[1].isBlank()) {
            log.warn("[ProxyController] Missing service prefix in path: {}", rawPath);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing service prefix in path.");
        }

        String prefix = segments[1].toLowerCase();
        String subPath = rawPath.substring(("/" + prefix).length());
        if (subPath.isEmpty()) subPath = "/";
        log.info("[ProxyController] Determined prefix: '{}', subPath: '{}'", prefix, subPath);

        String eurekaServiceName = resolver.resolveEurekaServiceName(prefix)
                .orElseThrow(() -> {
                    log.warn("[ProxyController] Unknown service prefix: {}", prefix);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service: " + prefix);
                });
        log.info("[ProxyController] Resolved Eureka service name: '{}' for prefix: '{}'", eurekaServiceName, prefix);

        HttpMethod method = request.getMethod() != null ? request.getMethod() : HttpMethod.GET;
        log.info("[ProxyController] Forwarding request: Method={}, Service={}, SubPath={}", method.name(), eurekaServiceName, subPath);
        
        // Pass original headers and body to the ApiGatewayService
        return apiGateway.forwardRequest(eurekaServiceName, subPath, method.name(), originalHeaders, requestBody);
    }

}
