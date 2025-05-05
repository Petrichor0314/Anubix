package com.petrichor.toggle_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ToggleController {

    @GetMapping({"", "/"})
    public ResponseEntity<String> getAllToggles() {
        return ResponseEntity.ok("List of toggles");
    }

    @GetMapping("/status")
    public ResponseEntity<String> getToggleStatus() {
        return ResponseEntity.ok("Toggle status is ACTIVE");
    }

    @GetMapping("/enable/{toggleName}")
    public ResponseEntity<String> enableToggle(@PathVariable String toggleName) {
        return ResponseEntity.ok("Enabled toggle: " + toggleName);
    }
}


