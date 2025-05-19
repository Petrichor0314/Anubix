package com.petrichor.loadbalancer.load_balancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class LoadBalancerConfig {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerConfig.class);

    @Value("${loadbalancer.port:8080}")
    private int port;

    @Value("${loadbalancer.algorithm:least-connections}")
    private String algorithm;

    @Value("${loadbalancer.retries:3}")
    private int retries;

    @Value("${LOADBALANCER_CACHE_TTL_SECONDS:60}")
    private int cacheTtlSeconds;

    @PostConstruct
    public void init() {
        logger.info("[LoadBalancerConfig] Initialized with algorithm: '{}', retries: {}, cache TTL: {}s", 
                    algorithm, retries, cacheTtlSeconds);
        if (algorithm == null || algorithm.trim().isEmpty()) {
            logger.warn("[LoadBalancerConfig] Load balancing algorithm is null or empty after injection. Default might be used or errors could occur.");
        } else if (algorithm.contains("#")) {
            logger.warn("[LoadBalancerConfig] WARNING: Detected '#' in algorithm string: '{}'. This is likely an issue from .env or config.", algorithm);
        }
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getRetries() {
        return retries;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }
}
