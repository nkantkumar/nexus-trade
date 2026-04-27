package com.trading.identity;

import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {

    private final JwtEncoder jwtEncoder;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();

    public IdentityService(JwtEncoder jwtEncoder, StringRedisTemplate redisTemplate) {
        this.jwtEncoder = jwtEncoder;
        this.redisTemplate = redisTemplate;
    }

    public AuthResult authenticate(LoginRequest request) {
        if (rateLimited(request.username())) {
            return new AuthResult.Rejected("Too many login attempts");
        }
        if (!request.password().equals("password")) {
            return new AuthResult.Rejected("Invalid credentials");
        }
        if (!TotpUtil.validate(request.totpCode(), "BASE32SECRET")) {
            return new AuthResult.Rejected("MFA validation failed");
        }
        String accessToken = issueAccessToken(request.username());
        String refreshToken = rotateRefreshToken(request.username(), null);
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("session:" + sessionId, request.username(), Duration.ofHours(8));
        return new AuthResult.Success(accessToken, refreshToken, sessionId, Instant.now().plus(Duration.ofHours(8)));
    }

    public String generateApiKeyHash(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String rotateRefreshToken(String username, String currentToken) {
        if (currentToken != null) {
            redisTemplate.delete("refresh:" + currentToken);
        }
        String nextToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("refresh:" + nextToken, username, Duration.ofDays(7));
        return nextToken;
    }

    private boolean rateLimited(String username) {
        int attempts = loginAttempts.computeIfAbsent(username, ignored -> new AtomicInteger()).incrementAndGet();
        return attempts > 5;
    }

    private String issueAccessToken(String username) {
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(username)
                .issuer("identity-service")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .claim("scope", "trade:read trade:write")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password, @NotBlank String totpCode) {}

    public sealed interface AuthResult permits AuthResult.Success, AuthResult.Rejected {
        record Success(String accessToken, String refreshToken, String sessionId, Instant sessionExpiresAt)
                implements AuthResult {}
        record Rejected(String reason) implements AuthResult {}
    }
}
