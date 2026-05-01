package com.trading.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtIssuerConfiguration {

    @Bean
    JwtEncoder jwtEncoder() {
        try {
            RSAKey rsaKey = new RSAKeyGenerator(2048).generate();
            JWKSet jwkSet = new JWKSet(rsaKey);
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
            return new NimbusJwtEncoder(jwkSource);
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate JWT signing key", e);
        }
    }
}
