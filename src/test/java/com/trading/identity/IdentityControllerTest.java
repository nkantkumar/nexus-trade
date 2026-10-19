package com.trading.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IdentityControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IdentityService realService = new IdentityService(null, null);
        IdentityController controller = new IdentityController(realService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/identity/me returns current user identity from JWT")
    void shouldReturnCurrentUserIdentity() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-100")
                .claim("preferred_username", "trader1")
                .claim("email", "trader1@nexus.trade")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority("ROLE_TRADER"),
                        new SimpleGrantedAuthority("SCOPE_trade:read")
                ),
                "trader1"
        );

        mockMvc.perform(get("/api/identity/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-100"))
                .andExpect(jsonPath("$.username").value("trader1"))
                .andExpect(jsonPath("$.email").value("trader1@nexus.trade"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_TRADER"))
                .andExpect(jsonPath("$.scopes[0]").value("SCOPE_trade:read"));
    }

    @Test
    @DisplayName("GET /api/identity/claims returns JWT claims")
    void shouldReturnJwtClaims() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-100")
                .claim("preferred_username", "trader1")
                .claim("custom_tag", "vip")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(), "trader1");

        mockMvc.perform(get("/api/identity/claims").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("user-100"))
                .andExpect(jsonPath("$.preferred_username").value("trader1"))
                .andExpect(jsonPath("$.custom_tag").value("vip"));
    }
}
