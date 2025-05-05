package com.petrichor.loadbalancer.load_balancer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadBalancerConfig {

    @Value("${loadbalancer.port:8080}")
    private int port;

    @Value("${loadbalancer.algorithm:least-connections}")
    private String algorithm;

    @Value("${loadbalancer.retries:3}")
    private int retries;


    public String getAlgorithm() {
        return algorithm;
    }

    public int getRetries() {
        return retries;
    }


}
