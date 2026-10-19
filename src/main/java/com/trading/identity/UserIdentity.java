package com.trading.identity;

import java.util.Set;

/**
 * Normalized user identity extracted from OAuth2 / OpenID Connect tokens.
 */
public record UserIdentity(
        String userId,
        String username,
        String email,
        Set<String> roles,
        Set<String> scopes
) {}
