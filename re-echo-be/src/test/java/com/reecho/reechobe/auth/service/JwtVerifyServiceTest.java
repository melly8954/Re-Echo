package com.reecho.reechobe.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtVerifyServiceTest {

    private static final String TEST_SECRET = "test-secret-change-me-test-secret-change-me";

    @Test
    void refresh_token의_사용자_식별자를_검증한다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtIssueService issueService = new JwtIssueService(properties);
        JwtVerifyService verifyService = new JwtVerifyService(properties);
        User user = User.createActive();
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        AuthToken token = issueService.issue(user);

        UUID result = verifyService.verifyRefreshToken(token.refreshToken());

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void access_token으로_refresh를_요청하면_예외를_던진다() {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtIssueService issueService = new JwtIssueService(properties);
        JwtVerifyService verifyService = new JwtVerifyService(properties);
        User user = User.createActive();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        AuthToken token = issueService.issue(user);

        assertThatThrownBy(() -> verifyService.verifyRefreshToken(token.accessToken()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    @Test
    void 만료된_refresh_token이면_예외를_던진다() throws Exception {
        JwtProperties properties = jwtProperties(Duration.ofDays(7));
        JwtVerifyService verifyService = new JwtVerifyService(properties);
        String refreshToken = expiredRefreshToken(UUID.randomUUID());

        assertThatThrownBy(() -> verifyService.verifyRefreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
    }

    private String expiredRefreshToken(UUID userId) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("re-echo")
                .subject(userId.toString())
                .issueTime(Date.from(now.minus(Duration.ofHours(2))))
                .expirationTime(Date.from(now.minus(Duration.ofHours(1))))
                .claim("typ", "refresh")
                .build();
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );
        signedJWT.sign(new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJWT.serialize();
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
