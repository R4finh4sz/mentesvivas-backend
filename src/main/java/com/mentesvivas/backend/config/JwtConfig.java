package com.mentesvivas.backend.config;

import com.mentesvivas.backend.exception.config.InvalidConfigurationException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, LoginLockoutProperties.class,
        PasswordRecoveryProperties.class, EmailProperties.class,
        EmailConfirmationProperties.class, CepProperties.class})
public class JwtConfig {

    public static final String ROLE_CLAIM = "role";

    private static final int KEY_MIN_BYTES = 32;

    @Bean
    SecretKey jwtSigningKey(JwtProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.secret() == null ? "" : properties.secret());
        } catch (IllegalArgumentException ex) {
            throw new InvalidConfigurationException("JWT_SECRET must be Base64.", ex);
        }
        if (bytes.length < KEY_MIN_BYTES) {
            throw new InvalidConfigurationException("JWT_SECRET must contain at least 32 random bytes in Base64.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(properties.issuer())));
        return decoder;
    }
}
