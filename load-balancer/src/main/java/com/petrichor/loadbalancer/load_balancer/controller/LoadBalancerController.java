package com.petrichor.loadbalancer.load_balancer.controller;

import com.petrichor.loadbalancer.load_balancer.service.LoadBalancerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
public class LoadBalancerController {

    private final LoadBalancerService loadBalancerService;

    public LoadBalancerController(LoadBalancerService loadBalancerService) {
        this.loadBalancerService = loadBalancerService;
    }

    @GetMapping("/api/test")
    public String forwardRequest() {
        return loadBalancerService.forwardRequest();
    }
}
