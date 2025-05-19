package com.petrichor.loadbalancer.load_balancer.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@Configuration
public class RateLimiterConfiguration {

    @Value("${RATELIMITER_LIMIT_FOR_PERIOD:10}")
    private int limitForPeriod;

    @Value("${RATELIMITER_REFRESH_PERIOD_SECONDS:1}")
    private long limitRefreshPeriodSeconds;

    @Value("${RATELIMITER_TIMEOUT_MS:500}")
    private long timeoutDurationMillis;

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod) // Max requests
                .limitRefreshPeriod(Duration.ofSeconds(limitRefreshPeriodSeconds)) // per period in seconds
                .timeoutDuration(Duration.ofMillis(timeoutDurationMillis)) // wait before failing
                .build();

        return RateLimiterRegistry.of(defaultConfig);
    }
}
