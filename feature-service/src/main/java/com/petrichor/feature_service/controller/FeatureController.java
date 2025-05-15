package com.petrichor.feature_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class FeatureController {

    @GetMapping({"", "/"})
    public Mono<String> getAllFeatures() {
        return Mono.just("List of features");
    }

    @GetMapping("/list")
    public Mono<String> getFeatureList() {
        return Mono.just("Feature list");
    }

    @GetMapping("/detail/{id}")
    public Mono<String> getFeatureDetail(@PathVariable String id) {
        return Mono.delay(Duration.ofMillis(500)).thenReturn("Feature detail for ID: " + id);
    }
}


