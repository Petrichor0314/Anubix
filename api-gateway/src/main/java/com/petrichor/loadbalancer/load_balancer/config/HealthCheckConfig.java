package com.petrichor.loadbalancer.load_balancer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthCheckConfig {

    @Value("${HEALTHCHECK_ENABLED:true}")
    private boolean enabled;

    @Value("${HEALTHCHECK_INTERVAL_MS:10000}")
    private long intervalMs;

    @Value("${HEALTHCHECK_PATH:/actuator/health}")
    private String path;

    @Value("${HEALTHCHECK_TIMEOUT_MS:2000}")
    private long timeoutMs;

    public boolean isEnabled() {
        return enabled;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public String getPath() {
        return path;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
} 