package com.m2nsteel.bank_program_modernization.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

    public TokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpirationTime,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpirationTime) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public String createAccessToken(String loginId, String externalId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(loginId)
                .claim("externalId", externalId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirationTime))
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken(String loginId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(loginId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpirationTime))
                .signWith(signingKey)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getLoginIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getExternalIdFromToken(String token) {
        return getClaims(token).get("externalId", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}