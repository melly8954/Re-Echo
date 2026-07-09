package com.reecho.reechobe.auth.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.reecho.reechobe.auth.config.JwtProperties;
import com.reecho.reechobe.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

// 내부 사용자 계정 기준으로 Re-Echo Access/Refresh Token을 발급한다.
@Service
@RequiredArgsConstructor
public class JwtIssueService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;

    // 사용자 식별자를 subject로 갖는 Access Token과 Refresh Token을 발급한다.
    public AuthToken issue(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("토큰을 발급할 사용자 식별자는 필수입니다.");
        }

        validateProperties();

        Instant issuedAt = Instant.now();
        Instant accessTokenExpiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        Instant refreshTokenExpiresAt = issuedAt.plus(properties.getRefreshTokenTtl());
        UUID userId = user.getId();

        return new AuthToken(
                encode(userId, ACCESS_TOKEN_TYPE, issuedAt, accessTokenExpiresAt),
                accessTokenExpiresAt,
                encode(userId, REFRESH_TOKEN_TYPE, issuedAt, refreshTokenExpiresAt),
                refreshTokenExpiresAt
        );
    }

    private String encode(
            UUID userId,
            String tokenType,
            Instant issuedAt,
            Instant expiresAt
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder().encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private JwtEncoder jwtEncoder() {
        String jwtSecret = properties.getSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT 비밀키 설정은 필수입니다.");
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT 비밀키는 32바이트 이상이어야 합니다.");
        }

        SecretKey secretKey = new SecretKeySpec(
                secretBytes,
                "HmacSHA256"
        );
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    private void validateProperties() {
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalStateException("JWT 발급자 설정은 필수입니다.");
        }

        if (properties.getAccessTokenTtl() == null) {
            throw new IllegalStateException("Access Token 만료 시간 설정은 필수입니다.");
        }

        if (properties.getRefreshTokenTtl() == null) {
            throw new IllegalStateException("Refresh Token 만료 시간 설정은 필수입니다.");
        }
    }
}
