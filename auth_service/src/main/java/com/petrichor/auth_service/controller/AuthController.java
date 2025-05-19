package com.petrichor.auth_service.controller;

import com.petrichor.auth_service.dto.AuthRequest;
import com.petrichor.auth_service.dto.AuthResponse;
import com.petrichor.auth_service.model.User;
import com.petrichor.auth_service.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final ReactiveAuthenticationManager authenticationManager;

    public AuthController(JwtUtil jwtUtil, ReactiveAuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        log.info("[AuthController] AuthController constructor called");
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest authRequest) {
        log.error("[AuthController] LOGIN METHOD ENTERED");
        log.debug("[AuthController] Received login request for username: {}", authRequest.getUsername());

        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(authRequest.getUsername(),
                authRequest.getPassword());

        log.debug("[AuthController] Attempting authentication for: {}", authRequest.getUsername());
        return authenticationManager.authenticate(authenticationToken)
                .flatMap(authentication -> {
                    log.debug("[AuthController] Authentication successful for principal: {}", authentication.getPrincipal());
                    if (!(authentication.getPrincipal() instanceof User)) {
                        log.error("[AuthController] Authentication principal is not an instance of User: {}", authentication.getPrincipal().getClass().getName());
                        // This case should ideally lead to an error response as well, maybe a 500 or a specific 401.
                        // For now, it will likely fail later or cause a ClassCastException.
                    }
                    User user = (User) authentication.getPrincipal(); // Potential ClassCastException
                    log.info("[AuthController] Authentication successful for user: {}", user.getUsername());
                    
                    log.debug("[AuthController] Attempting to generate token for user: {}", user.getUsername());
                    String token;
                    try {
                        token = jwtUtil.generateToken(user);
                        log.info("[AuthController] Token generated successfully for user: {}", user.getUsername());
                    } catch (Exception ex) {
                        log.error("[AuthController] Error during token generation for user {}: {}", user.getUsername(), ex.getMessage(), ex);
                        // Decide on error response, perhaps a 500
                        return Mono.error(new RuntimeException("Token generation failed", ex)); 
                    }
                    return Mono.just(ResponseEntity.ok(new AuthResponse(token)));
                })
                .onErrorResume(e -> {
                    // This specifically catches errors from the authenticationManager.authenticate() call
                    // or errors propagated by Mono.error() in the flatMap (like our token generation error).
                    log.error("[AuthController] Authentication or subsequent processing failed for username: {}. Error type: {}. Message: {}", 
                            authRequest.getUsername(), e.getClass().getSimpleName(), e.getMessage(), e);
                    
                    // Check if it's a Spring Security AuthenticationException for more specific handling
                    if (e instanceof org.springframework.security.core.AuthenticationException) {
                         ResponseEntity<AuthResponse> errorResponseEntity = ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(new AuthResponse("Authentication failed: " + e.getMessage())); // Provide error in response
                        return Mono.just(errorResponseEntity);
                    }
                    // For other errors (like our RuntimeException from token generation)
                    ResponseEntity<AuthResponse> errorResponseEntity = ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR) // Or a more specific error
                            .body(new AuthResponse("Login processing error: " + e.getMessage()));
                    return Mono.just(errorResponseEntity);
                })
                .doOnError(throwable -> {
                    // This is a final logger for any error that might not have been caught by onErrorResume
                    // or if onErrorResume itself throws an error (which it shouldn't with Mono.just).
                    log.error("[AuthController] Critical unhandled error during login for {}: {}",
                            authRequest.getUsername(), throwable.getMessage(), throwable);
                });
    }
}