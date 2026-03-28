package com.trishal.journalApp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    /** Must be at least 32 characters. Set in application.properties. */
    @Value("${spring.jwt.secret.key}")
    private String secretKey;

    private static final long TOKEN_VALIDITY_MS = 1000L * 60 * 60; // 1 hour

    // ── Public API ────────────────────────────────────────────────────────────

    public String generateToken(String userName) {
        return createToken(new HashMap<>(), userName);
    }

    public String extractUserName(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * BUG FIX: Previously only checked token expiry, ignoring whether the token's
     * subject matches the supplied userName. This meant a valid (non-expired) token
     * issued for user A would pass validation when presented as user B.
     *
     * Fix: the token is valid only if BOTH conditions hold —
     *   1. The subject claim matches the expected userName (case-sensitive).
     *   2. The token has not expired.
     */
    public boolean validateToken(String token, String userName) {
        final String tokenUserName = extractUserName(token);
        return tokenUserName.equals(userName) && !isTokenExpired(token);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setHeaderParam("typ", "JWT")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}