package com.reecho.reechobe.auth.refresh;

import java.time.Instant;
import java.util.UUID;

// Refresh Token의 유효 상태와 회전을 외부 저장소에 관리한다.
public interface RefreshTokenStore {

    void save(UUID userId, UUID tokenId, Instant expiresAt);

    boolean rotate(
            UUID userId,
            UUID currentTokenId,
            UUID newTokenId,
            Instant newExpiresAt
    );

    boolean revoke(UUID userId, UUID tokenId);
}
