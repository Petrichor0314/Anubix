package com.petrichor.loadbalancer.load_balancer.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/proxy")
public class LoadBalancerController {

    private final List<String> backendServers = List.of(
            "http://backend-server-1:8080/api/test",
            "http://backend-server-2:8080/api/test",
            "http://backend-server-3:8080/api/test"
    );

    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/api/test")
    public String forwardRequest() {
        int index = requestCounter.getAndIncrement() % backendServers.size();
        String targetUrl = backendServers.get(index);
        return restTemplate.getForObject(targetUrl, String.class);
    }
}
