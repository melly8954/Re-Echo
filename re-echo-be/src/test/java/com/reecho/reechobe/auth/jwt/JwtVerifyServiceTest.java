package com.reecho.reechobe.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reecho.reechobe.auth.config.JwtCodecConfig;
import com.reecho.reechobe.auth.config.JwtProperties;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

class JwtVerifyServiceTest {

    private static final String TEST_SECRET = "test-secret-change-me-test-secret-change-me";

    @Test
    void access_token의_사용자_식별자를_검증한다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtIssueService issueService = jwtIssueService(properties);
        JwtVerifyService verifyService = jwtVerifyService(properties);
        User user = User.createActive("사용자", null);
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        AuthToken token = issueService.issue(user);

        UUID result = verifyService.verifyAccessToken(token.accessToken());

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void refresh_token을_access_token으로_검증하면_예외를_던진다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtIssueService issueService = jwtIssueService(properties);
        JwtVerifyService verifyService = jwtVerifyService(properties);
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        AuthToken token = issueService.issue(user);

        assertThatThrownBy(() -> verifyService.verifyAccessToken(token.refreshToken()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void 만료된_access_token이면_인증_예외를_던진다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtVerifyService verifyService = jwtVerifyService(properties);
        String accessToken = expiredToken(properties, UUID.randomUUID(), "access");

        assertThatThrownBy(() -> verifyService.verifyAccessToken(accessToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void refresh_token의_사용자_식별자를_검증한다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtIssueService issueService = jwtIssueService(properties);
        JwtVerifyService verifyService = jwtVerifyService(properties);
        User user = User.createActive("사용자", null);
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        AuthToken token = issueService.issue(user);

        VerifiedToken result = verifyService.verifyRefreshToken(token.refreshToken());

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tokenId()).isEqualTo(token.refreshTokenId());
        assertThat(result.expiresAt().getEpochSecond())
                .isEqualTo(token.refreshTokenExpiresAt().getEpochSecond());
    }

    @Test
    void access_token으로_refresh를_요청하면_예외를_던진다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtIssueService issueService = jwtIssueService(properties);
        JwtVerifyService verifyService = jwtVerifyService(properties);
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        AuthToken token = issueService.issue(user);

        assertThatThrownBy(() -> verifyService.verifyRefreshToken(token.accessToken()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    @Test
    void 만료된_refresh_token이면_예외를_던진다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtVerifyService verifyService = jwtVerifyService(properties);
        String refreshToken = expiredToken(properties, UUID.randomUUID(), "refresh");

        assertThatThrownBy(() -> verifyService.verifyRefreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
    }

    private String expiredToken(JwtProperties properties, UUID userId, String tokenType) {
        Instant now = Instant.now();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now.minus(Duration.ofHours(2)))
                .expiresAt(now.minus(Duration.ofHours(1)))
                .claim("typ", tokenType)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder(properties)
                .encode(JwtEncoderParameters.from(header, claimsSet))
                .getTokenValue();
    }

    private JwtIssueService jwtIssueService(JwtProperties properties) {
        return new JwtIssueService(properties, jwtEncoder(properties));
    }

    private JwtVerifyService jwtVerifyService(JwtProperties properties) {
        JwtCodecConfig codecConfig = new JwtCodecConfig();
        return new JwtVerifyService(
                properties,
                codecConfig.jwtDecoder(codecConfig.jwtSecretKey(properties))
        );
    }

    private JwtEncoder jwtEncoder(JwtProperties properties) {
        JwtCodecConfig codecConfig = new JwtCodecConfig();
        return codecConfig.jwtEncoder(codecConfig.jwtSecretKey(properties));
    }

    private JwtProperties jwtProperties(Duration refreshTokenTtl) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("re-echo");
        properties.setSecret(TEST_SECRET);
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(refreshTokenTtl);
        return properties;
    }
}
