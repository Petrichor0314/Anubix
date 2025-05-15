package com.petrichor.analytics_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class AnalyticsController {
    @GetMapping("/")
    public Mono<String> getAnalytics() {
        return Mono.just("Analytics data");
    }

    @GetMapping("/stats")
    public Mono<String> getAnalyticsStats() {
        return Mono.just("Analytics statistics");
    }

    // Example with simulated delay (non-blocking)
    @GetMapping("/report")
    public Mono<String> getAnalyticsReport() {
        return Mono.delay(Duration.ofMillis(100)) // Simulate async op
                .thenReturn("Analytics report");
    }
}


