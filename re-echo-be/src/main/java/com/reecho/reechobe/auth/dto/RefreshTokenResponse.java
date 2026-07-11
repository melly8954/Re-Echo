package com.reecho.reechobe.auth.dto;

import java.time.Instant;

// Access Token을 URL이 아닌 응답 body로 전달한다.
public record RefreshTokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt
) {
}
