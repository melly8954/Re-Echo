package com.reecho.reechobe.auth.handler;

import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Refresh Token을 클라이언트 스크립트가 읽을 수 없는 쿠키로 전달한다.
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieWriter {

    private final OAuth2LoginProperties properties;

    // 토큰 재발급 시에도 동일한 쿠키 정책을 유지하기 위해 공통 writer로 둔다.
    public void add(HttpServletResponse response, AuthToken token) {
        validateProperties();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie(token).toString());
    }

    private ResponseCookie refreshTokenCookie(AuthToken token) {
        return ResponseCookie.from(properties.getRefreshTokenCookieName(), token.refreshToken())
                .httpOnly(true)
                .secure(properties.isRefreshTokenSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.between(Instant.now(), token.refreshTokenExpiresAt()))
                .build();
    }

    private void validateProperties() {
        if (properties.getRefreshTokenCookieName() == null || properties.getRefreshTokenCookieName().isBlank()) {
            throw new IllegalStateException("Refresh Token 쿠키 이름 설정은 필수입니다.");
        }
    }
}
