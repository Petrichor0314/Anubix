package com.petrichor.analytics_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    @GetMapping({"", "/"})
    public ResponseEntity<String> getAnalytics() {
        return ResponseEntity.ok("Analytics data");
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getAnalyticsStats() {
        return ResponseEntity.ok("Analytics statistics");
    }

    @GetMapping("/report")
    public ResponseEntity<String> getAnalyticsReport() {
        return ResponseEntity.ok("Analytics report");
    }
}


