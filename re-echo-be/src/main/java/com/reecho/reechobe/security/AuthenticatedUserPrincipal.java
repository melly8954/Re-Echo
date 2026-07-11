package com.reecho.reechobe.security;

import java.util.UUID;

// Access Token 검증 후 Security Context에 저장할 사용자 식별 정보다.
public record AuthenticatedUserPrincipal(UUID userId) {

    public AuthenticatedUserPrincipal {
        if (userId == null) {
            throw new IllegalArgumentException("인증 사용자 식별자는 필수입니다.");
        }
    }
}
