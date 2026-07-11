package com.reecho.reechobe.infra.redis;

import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

// Redis에서 다중 세션별 Refresh Token 상태를 원자적으로 관리한다.
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            redis.call('PSETEX', KEYS[2], ARGV[2], ARGV[1])
            return 1
            """,
            Long.class
    );
    private static final DefaultRedisScript<Long> REVOKE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            return 1
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    // 로그인 시 세션별 Refresh Token 식별자를 만료 시간까지 저장한다.
    @Override
    public void save(UUID userId, UUID tokenId, Instant expiresAt) {
        redisTemplate.opsForValue().set(
                key(tokenId),
                userId.toString(),
                ttl(expiresAt)
        );
    }

    // 현재 토큰이 유효한 경우에만 기존 키를 새 토큰 키로 교체한다.
    @Override
    public boolean rotate(
            UUID userId,
            UUID currentTokenId,
            UUID newTokenId,
            Instant newExpiresAt
    ) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(currentTokenId), key(newTokenId)),
                userId.toString(),
                String.valueOf(ttl(newExpiresAt).toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    // 사용자와 토큰 식별자가 일치하는 세션만 무효화한다.
    @Override
    public boolean revoke(UUID userId, UUID tokenId) {
        Long result = redisTemplate.execute(
                REVOKE_SCRIPT,
                List.of(key(tokenId)),
                userId.toString()
        );
        return Long.valueOf(1L).equals(result);
    }

    private String key(UUID tokenId) {
        return KEY_PREFIX + tokenId;
    }

    private Duration ttl(Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Refresh Token 만료 시간은 현재보다 이후여야 합니다.");
        }
        return ttl;
    }
}
