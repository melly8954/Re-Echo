package com.reecho.reechobe.auth.jwt;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.config.JwtProperties;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

// Re-Echo JWT의 서명과 Refresh Token 용도를 검증한다.
@Service
@RequiredArgsConstructor
public class JwtVerifyService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;
    private final JwtDecoder jwtDecoder;

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
            Jwt jwt = jwtDecoder.decode(token);
            validateClaims(jwt, expectedTokenType, invalidErrorCode, expiredErrorCode);
            return UUID.fromString(jwt.getSubject());
        } catch (BusinessException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private void validateClaims(
            Jwt jwt,
            String expectedTokenType,
            ErrorCode invalidErrorCode,
            ErrorCode expiredErrorCode
    ) {
        if (!properties.getIssuer().equals(jwt.getClaimAsString(JwtClaimNames.ISS))) {
            throw new BusinessException(invalidErrorCode);
        }

        if (!expectedTokenType.equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM))) {
            throw new BusinessException(invalidErrorCode);
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw new BusinessException(invalidErrorCode);
        }

        if (!expiresAt.isAfter(Instant.now())) {
            throw new BusinessException(expiredErrorCode);
        }

        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new BusinessException(invalidErrorCode);
        }
    }
}
