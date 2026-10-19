package com.trading.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class IdentityServiceTest {

    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        // Test pure methods without requiring Redis bytecode instrumentation
        identityService = new IdentityService(null, null);
    }

    @Test
    @DisplayName("Should extract UserIdentity from JwtAuthenticationToken")
    void shouldExtractUserIdentityFromJwtToken() {
        Jwt jwt = Jwt.withTokenValue("sample-token")
                .header("alg", "RS256")
                .subject("uuid-123")
                .claim("preferred_username", "trader_joe")
                .claim("email", "joe@nexus.trade")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_TRADER"),
                        new SimpleGrantedAuthority("SCOPE_trade:read")
                ),
                "trader_joe"
        );

        UserIdentity identity = identityService.getCurrentUser(auth);

        assertThat(identity.userId()).isEqualTo("uuid-123");
        assertThat(identity.username()).isEqualTo("trader_joe");
        assertThat(identity.email()).isEqualTo("joe@nexus.trade");
        assertThat(identity.roles()).containsExactly("ROLE_TRADER");
        assertThat(identity.scopes()).containsExactly("SCOPE_trade:read");
    }

    @Test
    @DisplayName("Should generate deterministic SHA-256 API key hash")
    void shouldGenerateApiKeyHash() {
        String hash1 = identityService.generateApiKeyHash("my-secret-key-123");
        String hash2 = identityService.generateApiKeyHash("my-secret-key-123");

        assertThat(hash1).isNotEmpty();
        assertThat(hash1).isEqualTo(hash2);
    }
}
