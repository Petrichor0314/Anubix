package com.petrichor.loadbalancer.load_balancer.config;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;


@Configuration
public class Resilience4jConfig {

    // List of HTTP status codes that should trigger a retry
    private static final List<HttpStatus> RETRYABLE_STATUSES = Arrays.asList(
            HttpStatus.INTERNAL_SERVER_ERROR,  // 500
            HttpStatus.BAD_GATEWAY,            // 502
            HttpStatus.SERVICE_UNAVAILABLE,    // 503
            HttpStatus.GATEWAY_TIMEOUT         // 504
    );

    // Predicate to check if an exception is retryable
    private static final Predicate<Throwable> RETRYABLE_EXCEPTIONS = throwable -> {
        if (throwable instanceof ResponseStatusException) {
            HttpStatusCode statusCode = ((ResponseStatusException) throwable).getStatusCode();
            // Compare status code values directly
            return RETRYABLE_STATUSES.stream()
                    .anyMatch(httpStatus -> httpStatus.value() == statusCode.value());
        }
        return true; // Retry on other exceptions
    };

    @Value("${R4J_RETRY_MAX_ATTEMPTS:3}")
    private int maxAttempts;

    @Value("${R4J_RETRY_WAIT_DURATION_MS:500}")
    private long waitDurationMillis;

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(java.time.Duration.ofMillis(waitDurationMillis))
                .retryOnException(RETRYABLE_EXCEPTIONS)  // Only retry on retryable errors
                .failAfterMaxAttempts(true)  // Propagate the last error after max attempts
                .build();
        return RetryRegistry.of(config);
    }
}