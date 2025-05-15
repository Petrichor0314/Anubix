package com.petrichor.toggle_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class ToggleController {

    @GetMapping({"", "/"})
    public Mono<String> getAllToggles() {
        return Mono.just("List of toggles");
    }

    @GetMapping("/status")
    public Mono<String> getToggleStatus() {
        return Mono.just("Toggle status is ACTIVE");
    }

    @GetMapping("/enable/{toggleName}")
    public Mono<String> enableToggle(@PathVariable String toggleName) {
        return Mono.delay(Duration.ofMinutes(1)).thenReturn("Enabled toggle: " + toggleName);
    }
}


