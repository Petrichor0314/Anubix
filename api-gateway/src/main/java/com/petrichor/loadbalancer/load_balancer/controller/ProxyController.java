package com.petrichor.loadbalancer.load_balancer.controller;

import com.petrichor.loadbalancer.load_balancer.service.ApiGatewayService;
import com.petrichor.loadbalancer.load_balancer.util.ServiceNameResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
public class ProxyController {

    private final ApiGatewayService apiGateway;
    private final ServiceNameResolver resolver;

    public ProxyController(ApiGatewayService apiGateway, ServiceNameResolver resolver) {
        this.apiGateway = apiGateway;
        this.resolver = resolver;
    }

    @RequestMapping("/**")
    public Mono<String> routeRequest(ServerHttpRequest request) {
        String rawPath = request.getPath().pathWithinApplication().value(); // e.g. /features/hello
        String[] segments = rawPath.split("/");

        if (segments.length < 2 || segments[1].isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing service prefix in path.");
        }

        String prefix = segments[1].toLowerCase();
        String subPath = rawPath.substring(("/" + prefix).length());
        if (subPath.isEmpty()) subPath = "/";

        String eurekaServiceName = resolver.resolveEurekaServiceName(prefix)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service: " + prefix));

        HttpMethod method = request.getMethod() != null ? request.getMethod() : HttpMethod.GET;
        return apiGateway.forwardRequest(eurekaServiceName, subPath, method.name());
    }

}
