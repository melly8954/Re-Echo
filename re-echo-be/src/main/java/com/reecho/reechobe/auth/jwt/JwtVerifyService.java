package com.reecho.reechobe.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.config.JwtProperties;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Re-Echo JWT의 서명과 Refresh Token 용도를 검증한다.
@Service
@RequiredArgsConstructor
public class JwtVerifyService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;

    // Access Token이 유효하면 subject에 담긴 사용자 식별자를 반환한다.
    public UUID verifyAccessToken(String accessToken) {
        return verifyToken(
                accessToken,
                ACCESS_TOKEN_TYPE,
                AuthErrorCode.AUTH_UNAUTHORIZED,
                AuthErrorCode.AUTH_UNAUTHORIZED
        );
    }

    // Refresh Token이 유효하면 subject에 담긴 사용자 식별자를 반환한다.
    public UUID verifyRefreshToken(String refreshToken) {
        return verifyToken(
                refreshToken,
                REFRESH_TOKEN_TYPE,
                AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID,
                AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED
        );
    }

    private UUID verifyToken(
            String token,
            String expectedTokenType,
            ErrorCode invalidErrorCode,
            ErrorCode expiredErrorCode
    ) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(invalidErrorCode);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(new MACVerifier(secretBytes()))) {
                throw new BusinessException(invalidErrorCode);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            validateClaims(claims, expectedTokenType, invalidErrorCode, expiredErrorCode);
            return UUID.fromString(claims.getSubject());
        } catch (BusinessException exception) {
            throw exception;
        } catch (JOSEException | ParseException | IllegalArgumentException exception) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private void validateClaims(
            JWTClaimsSet claims,
            String expectedTokenType,
            ErrorCode invalidErrorCode,
            ErrorCode expiredErrorCode
    ) throws ParseException {
        if (!properties.getIssuer().equals(claims.getIssuer())) {
            throw new BusinessException(invalidErrorCode);
        }

        if (!expectedTokenType.equals(claims.getStringClaim(TOKEN_TYPE_CLAIM))) {
            throw new BusinessException(invalidErrorCode);
        }

        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null) {
            throw new BusinessException(invalidErrorCode);
        }

        if (!expirationTime.toInstant().isAfter(Instant.now())) {
            throw new BusinessException(expiredErrorCode);
        }

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private byte[] secretBytes() {
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalStateException("JWT 발급자 설정은 필수입니다.");
        }

        String jwtSecret = properties.getSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT 비밀키 설정은 필수입니다.");
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT 비밀키는 32바이트 이상이어야 합니다.");
        }
        return secretBytes;
    }
}
