package com.reecho.reechobe.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void refresh_token을_세션별_키와_ttl로_저장한다() {
        RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        store.save(userId, tokenId, Instant.now().plusSeconds(60));

        verify(valueOperations).set(
                eq("auth:refresh:" + tokenId),
                eq(userId.toString()),
                ttlCaptor.capture()
        );
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redis_스크립트가_성공하면_refresh_token_회전에_성공한다() {
        RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(1L);

        boolean result = store.rotate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(60)
        );

        assertThat(result).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redis_스크립트가_거부하면_refresh_token_폐기에_실패한다() {
        RedisRefreshTokenStore store = new RedisRefreshTokenStore(redisTemplate);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(0L);

        boolean result = store.revoke(UUID.randomUUID(), UUID.randomUUID());

        assertThat(result).isFalse();
    }
}
