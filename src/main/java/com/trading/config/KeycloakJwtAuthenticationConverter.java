package com.trading.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Converts Keycloak JWT access tokens into Spring Security authentication tokens.
 * Extracts:
 * 1. Standard OAuth2 scopes ("scope" / "scp") -> SCOPE_<name>
 * 2. Keycloak realm roles ("realm_access.roles") -> ROLE_<ROLE_NAME>
 * 3. Keycloak client roles ("resource_access.<client>.roles") -> ROLE_<CLIENT>_<ROLE_NAME>
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultAuthoritiesConverter.convert(jwt);
        Set<GrantedAuthority> keycloakAuthorities = extractRealmAndResourceRoles(jwt);

        Set<GrantedAuthority> combinedAuthorities = Stream.concat(
                (defaultAuthorities != null ? defaultAuthorities.stream() : Stream.empty()),
                keycloakAuthorities.stream()
        ).collect(Collectors.toSet());

        String principalClaimName = jwt.hasClaim("preferred_username") ? "preferred_username" : "sub";
        String principal = jwt.getClaimAsString(principalClaimName);

        return new JwtAuthenticationToken(jwt, combinedAuthorities, principal != null ? principal : jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Set<GrantedAuthority> extractRealmAndResourceRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Realm-level roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleStr && !roleStr.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleStr.toUpperCase()));
                }
            }
        }

        // 2. Client-level resource roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                String clientName = entry.getKey();
                if (entry.getValue() instanceof Map<?, ?> clientAccess) {
                    Object clientRoles = clientAccess.get("roles");
                    if (clientRoles instanceof Collection<?> roles) {
                        for (Object role : roles) {
                            if (role instanceof String roleStr && !roleStr.isBlank()) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + clientName.toUpperCase() + "_" + roleStr.toUpperCase()));
                            }
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableSet(authorities);
    }
}
