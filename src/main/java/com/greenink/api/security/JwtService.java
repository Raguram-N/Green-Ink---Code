package com.greenink.api.security;

import com.greenink.api.config.GreenInkProperties;
import com.greenink.api.user.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private final SecretKey key;
    private final GreenInkProperties properties;

    public JwtService(GreenInkProperties properties) {
        this.properties = properties;
        String secret = properties.security().jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("GREEN_INK_JWT_SECRET must be at least 32 bytes. Use the dev profile locally.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expires = now.plus(properties.security().accessTokenTtl());
        return Jwts.builder()
                .subject(user.id())
                .claim("roles", user.roles())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(key)
                .compact();
    }

    public ParsedAccessToken parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Not an access token");
        }
        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null ? List.of() : rawRoles.stream().map(String::valueOf).toList();
        return new ParsedAccessToken(claims.getSubject(), roles, claims.getExpiration().toInstant());
    }

    public long accessTokenExpiresInSeconds() {
        return properties.security().accessTokenTtl().toSeconds();
    }

    public record ParsedAccessToken(String userId, List<String> roles, Instant expiresAt) {}
}
