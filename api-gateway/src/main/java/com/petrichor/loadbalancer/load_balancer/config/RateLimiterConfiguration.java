package com.petrichor.loadbalancer.load_balancer.config;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(10) // Max 10 requests
                .limitRefreshPeriod(Duration.ofSeconds(1)) // per 1 second
                .timeoutDuration(Duration.ofMillis(500)) // wait 500ms before failing
                .build();

        return RateLimiterRegistry.of(defaultConfig);
    }
}
