package com.reecho.reechobe.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// JWT 서명 키와 Spring Security Encoder·Decoder를 한 번만 구성한다.
@Configuration
public class JwtCodecConfig {

    // 설정된 비밀키를 HS256 서명에 사용할 키로 변환한다.
    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        String jwtSecret = properties.getSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT 비밀키 설정은 필수입니다.");
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT 비밀키는 32바이트 이상이어야 합니다.");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    // 애플리케이션이 Spring Security 추상화로 JWT를 발급하도록 구성한다.
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<SecurityContext>(jwtSecretKey)
        );
    }

    // 서명 검증은 Decoder가 담당하고 토큰별 Claims 정책은 검증 서비스에 위임한다.
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return jwtDecoder;
    }
}
