package com.reecho.reechobe.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.config.JwtProperties;
import com.reecho.reechobe.common.exception.BusinessException;
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
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;

    // Refresh Token이 유효하면 subject에 담긴 사용자 식별자를 반환한다.
    public UUID verifyRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            if (!signedJWT.verify(new MACVerifier(secretBytes()))) {
                throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            validateClaims(claims);
            return UUID.fromString(claims.getSubject());
        } catch (BusinessException exception) {
            throw exception;
        } catch (JOSEException | ParseException | IllegalArgumentException exception) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
    }

    private void validateClaims(JWTClaimsSet claims) throws ParseException {
        if (!properties.getIssuer().equals(claims.getIssuer())) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        if (!REFRESH_TOKEN_TYPE.equals(claims.getStringClaim(TOKEN_TYPE_CLAIM))) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        if (!expirationTime.toInstant().isAfter(Instant.now())) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
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
