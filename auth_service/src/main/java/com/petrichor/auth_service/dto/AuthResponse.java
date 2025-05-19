package com.petrichor.auth_service.dto;

public class AuthResponse {
    private String token;

    // No-args constructor
    public AuthResponse() {
    }

    // All-args constructor
    public AuthResponse(String token) {
        this.token = token;
    }

    // Getter
    public String getToken() {
        return token;
    }

    // Setter
    public void setToken(String token) {
        this.token = token;
    }
} 