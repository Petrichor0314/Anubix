package com.petrichor.feature_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.time.Duration;

@RestController
public class FeatureController {

    private static final Logger logger = LoggerFactory.getLogger(FeatureController.class);
    private String hostname = "unknown-host";

    public FeatureController() {
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Could not get hostname", e);
        }
    }

    @GetMapping({"", "/"})
    public Mono<String> getAllFeatures() {
        logger.info("FeatureService instance '{}' received request for getAllFeatures", hostname);
        return Mono.just("List of features");
    }

    @GetMapping("/list")
    public Mono<String> getFeatureList() {
        logger.info("FeatureService instance '{}' received request for getFeatureList", hostname);
        return Mono.just("Feature list");
    }

    @GetMapping("/detail/{id}")
    public Mono<String> getFeatureDetail(@PathVariable String id) {
        logger.info("FeatureService instance '{}' received request for getFeatureDetail with id: {}", hostname, id);
        return Mono.delay(Duration.ofMillis(500)).thenReturn("Feature detail for ID: " + id);
    }
}


