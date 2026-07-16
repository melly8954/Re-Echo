package com.reecho.reechobe.realtime.security;

import java.security.Principal;
import java.util.UUID;

// STOMP session에 JWT 검증 결과인 사용자 식별자를 보존한다.
public record StompUserPrincipal(UUID userId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
