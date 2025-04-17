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

    @Value("${loadbalancer.retry-delay:1000}")
    private int retryDelay;

    @Value("${loadbalancer.healthcheck.enabled:true}")
    private boolean healthCheckEnabled;

    @Value("${loadbalancer.healthcheck.interval:5000}")
    private int healthCheckInterval;

    @Value("${loadbalancer.healthcheck.path:/health}")
    private String healthCheckPath;

    @Value("${loadbalancer.healthcheck.retries:3}")
    private int healthCheckRetries;

    @Value("${loadbalancer.healthcheck.retry-delay:1000}")
    private int healthCheckRetryDelay;

    @Value("${loadbalancer.healthcheck.timeout:2}")
    private int healthCheckTimeoutSeconds;

    // Getters
    public int getPort() {
        return port;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getRetries() {
        return retries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    public int getHealthCheckRetries() {
        return healthCheckRetries;
    }

    public int getHealthCheckRetryDelayMillis() {
        return healthCheckRetryDelay;
    }

    public int getHealthCheckTimeoutSeconds() {
        return healthCheckTimeoutSeconds;
    }
}
