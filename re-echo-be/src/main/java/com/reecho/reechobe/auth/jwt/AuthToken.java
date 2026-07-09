package com.reecho.reechobe.auth.jwt;

import java.time.Instant;
import java.util.UUID;

// 로그인 성공 후 클라이언트에 전달하거나 쿠키에 저장할 토큰 묶음이다.
public record AuthToken(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UUID refreshTokenId,
        Instant refreshTokenExpiresAt
) {
}
