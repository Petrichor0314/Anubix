package com.petrichor.loadbalancer.load_balancer.config;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


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

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(java.time.Duration.ofMillis(500))
                .retryOnException(RETRYABLE_EXCEPTIONS)  // Only retry on retryable errors
                .failAfterMaxAttempts(true)  // Propagate the last error after max attempts
                .build();
        return RetryRegistry.of(config);
    }
}