package com.trading.identity;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for user identity and token introspection in the OAuth2/OIDC realm.
 */
@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    /**
     * Returns the normalized profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public UserIdentity me(Authentication authentication) {
        return identityService.getCurrentUser(authentication);
    }

    /**
     * Returns the raw claims from the Keycloak JWT token (useful for inspection/debugging).
     */
    @GetMapping("/claims")
    public Map<String, Object> claims(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaims();
        }
        return Map.of("principal", authentication != null ? authentication.getName() : "anonymous");
    }

    /**
     * Endpoint accessible only to users with the TRADER role.
     */
    @GetMapping("/trader-check")
    @PreAuthorize("hasRole('TRADER')")
    public Map<String, String> traderCheck(Authentication authentication) {
        return Map.of(
                "status", "authorized",
                "message", "Trader access confirmed",
                "user", authentication != null ? authentication.getName() : "unknown"
        );
    }

    /**
     * Endpoint accessible only to users with the ADMIN role.
     */
    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminCheck(Authentication authentication) {
        return Map.of(
                "status", "authorized",
                "message", "Admin access confirmed",
                "user", authentication != null ? authentication.getName() : "unknown"
        );
    }
}
