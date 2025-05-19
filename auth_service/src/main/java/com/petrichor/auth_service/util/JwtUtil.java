package com.petrichor.auth_service.util;

import com.petrichor.auth_service.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretString; // Store as base64 encoded string in properties

    @Value("${jwt.expiration}")
    private Long expirationTime; // in milliseconds

    private SecretKey key;

    @PostConstruct
    public void init() {
        // Ensure the secret is strong enough for HS256, or use a more appropriate algorithm
        // For HS256, the key must be at least 256 bits (32 bytes)
        // If your secretString is shorter, this might lead to weak keys.
        // Consider generating a strong key and storing it securely.
        byte[] keyBytes = Base64.getDecoder().decode(secretString);
        if (keyBytes.length < 32) {
             // Pad or use a key derivation function if necessary, or simply ensure a strong enough secret
             // For simplicity, we'll use it as is, but log a warning for production.
             System.err.println("Warning: JWT secret key is less than 256 bits. Consider using a stronger key.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("userId", user.getId());
        // Add other claims as needed
        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    public Boolean validateToken(String token, User userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    
    public Boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
} 