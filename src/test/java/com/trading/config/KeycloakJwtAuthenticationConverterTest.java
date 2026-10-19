package com.trading.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter();
    }

    @Test
    @DisplayName("Should extract realm roles, client resource roles, scopes, and preferred_username")
    void shouldExtractAllRolesAndScopes() {
        Jwt jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "RS256")
                .subject("user-uuid-12345")
                .claim("preferred_username", "alex.trader")
                .claim("email", "alex@nexus.trade")
                .claim("scope", "trade:read trade:write")
                .claim("realm_access", Map.of("roles", List.of("TRADER", "default-roles-nexus-trade")))
                .claim("resource_access", Map.of(
                        "nexus-client", Map.of("roles", List.of("order-entry"))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        AbstractAuthenticationToken authToken = converter.convert(jwt);

        assertThat(authToken).isNotNull();
        assertThat(authToken.getName()).isEqualTo("alex.trader");

        Set<String> authorities = authToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains(
                "ROLE_TRADER",
                "ROLE_DEFAULT-ROLES-NEXUS-TRADE",
                "ROLE_NEXUS-CLIENT_ORDER-ENTRY",
                "SCOPE_trade:read",
                "SCOPE_trade:write"
        );
    }

    @Test
    @DisplayName("Should fall back to subject when preferred_username is absent")
    void shouldFallbackToSubjectWhenUsernameAbsent() {
        Jwt jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "RS256")
                .subject("service-account-sub-999")
                .claim("realm_access", Map.of("roles", List.of("service-bot")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        AbstractAuthenticationToken authToken = converter.convert(jwt);

        assertThat(authToken).isNotNull();
        assertThat(authToken.getName()).isEqualTo("service-account-sub-999");

        Set<String> authorities = authToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_SERVICE-BOT");
    }
}
