package com.petrichor.toggle_service.controller;

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
public class ToggleController {

    private static final Logger logger = LoggerFactory.getLogger(ToggleController.class);
    private String hostname = "unknown-host";

    public ToggleController() {
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Could not get hostname", e);
        }
    }

    @GetMapping({"", "/"})
    public Mono<String> getToggles() {
        logger.info("ToggleService instance '{}' received request for getToggles", hostname);
        return Mono.just("List of toggles");
    }

    @GetMapping("/list")
    public Mono<String> getToggleList() {
        logger.info("ToggleService instance '{}' received request for getToggleList", hostname);
        return Mono.just("Toggle list");
    }

    @GetMapping("/detail/{id}")
    public Mono<String> getToggleDetail(@PathVariable("id") String id) {
        logger.info("ToggleService instance '{}' received request for getToggleDetail with id: {}", hostname, id);
        return Mono.delay(Duration.ofMillis(300)).thenReturn("Toggle detail for ID: " + id);
    }

    @GetMapping("/status")
    public Mono<String> getToggleStatus() {
        logger.info("ToggleService instance '{}' received request for getToggleStatus", hostname);
        return Mono.just("Toggle status: active");
    }

    @GetMapping("/enable/{toggleName}")
    public Mono<String> enableToggle(@PathVariable("toggleName") String toggleName) {
        return Mono.delay(Duration.ofMinutes(1)).thenReturn("Enabled toggle: " + toggleName);
    }
}


