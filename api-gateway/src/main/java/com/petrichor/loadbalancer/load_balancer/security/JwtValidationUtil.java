package com.petrichor.loadbalancer.load_balancer.security;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys; // Correct import for JJWT 0.12.x
import io.jsonwebtoken.security.SignatureException; // Import Logger
import jakarta.annotation.PostConstruct; // Import LoggerFactory

@Component
public class JwtValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationUtil.class); // Add logger

    @Value("${jwt.secret}") // This needs to be the same secret as in auth_service
    private String secretString;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretString);
         if (keyBytes.length < 32) {
             System.err.println("Warning: JWT secret key in API Gateway is less than 256 bits. Ensure it matches auth_service and is strong.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }
     public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", String.class);
    }


    public boolean validateToken(String token) {
        try {
            extractAllClaims(token); // If this doesn't throw, token is structurally valid and signature is okay
            return !isTokenExpired(token);
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token (malformed): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // This can happen if the token string is null or empty, or if Claims JWS is invalid
            log.error("JWT claims string is empty or invalid: {}", e.getMessage());
        } catch (Exception e) { // Catch-all for any other unexpected exceptions
            log.error("Unexpected error during JWT token validation: {}", e.getMessage(), e);
        }
        return false;
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
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