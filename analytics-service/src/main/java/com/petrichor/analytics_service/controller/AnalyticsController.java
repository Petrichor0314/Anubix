package com.petrichor.analytics_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.time.Duration;

@RestController
public class AnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    private String hostname = "unknown-host";

    public AnalyticsController() {
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Could not get hostname", e);
        }
    }

    @GetMapping("/")
    public Mono<String> getAnalytics() {
        logger.info("AnalyticsService instance '{}' received request for getAnalytics", hostname);
        return Mono.just("Analytics data");
    }

    @GetMapping("/stats")
    public Mono<String> getAnalyticsStats() {
        logger.info("AnalyticsService instance '{}' received request for getAnalyticsStats", hostname);
        return Mono.just("Analytics statistics");
    }

    // Example with simulated delay (non-blocking)
    @GetMapping("/report")
    public Mono<String> getAnalyticsReport() {
        logger.info("AnalyticsService instance '{}' received request for getAnalyticsReport", hostname);
        return Mono.delay(Duration.ofMillis(100)) // Simulate async op
                .thenReturn("Analytics report");
    }
}


