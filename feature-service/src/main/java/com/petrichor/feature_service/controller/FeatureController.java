package com.petrichor.feature_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureController {

    @GetMapping({"", "/"})
    public ResponseEntity<String> getAllFeatures() {
        return ResponseEntity.ok("List of features");
    }

    @GetMapping("/list")
    public ResponseEntity<String> getFeatureList() {
        return ResponseEntity.ok("Feature list");
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<String> getFeatureDetail(@PathVariable String id) {
        return ResponseEntity.ok("Feature detail for ID: " + id);
    }
}

