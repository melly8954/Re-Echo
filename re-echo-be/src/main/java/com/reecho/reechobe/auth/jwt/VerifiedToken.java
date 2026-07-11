package com.reecho.reechobe.auth.jwt;

import java.time.Instant;
import java.util.UUID;

// 검증을 통과한 JWT에서 서버 상태 확인에 필요한 값을 표현한다.
public record VerifiedToken(
        UUID userId,
        UUID tokenId,
        Instant expiresAt
) {

    public VerifiedToken {
        if (userId == null || tokenId == null || expiresAt == null) {
            throw new IllegalArgumentException("검증된 토큰 정보는 모두 필수입니다.");
        }
    }
}
