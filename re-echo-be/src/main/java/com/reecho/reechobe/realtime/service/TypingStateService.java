package com.reecho.reechobe.realtime.service;

import com.reecho.reechobe.realtime.config.WebSocketProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// Redis 만료 시각을 기준으로 입력 중 멤버 snapshot을 유지하고 전파한다.
@Service
@RequiredArgsConstructor
public class TypingStateService {

    private static final String KEY_PREFIX = "reecho:typing:";

    private final StringRedisTemplate redisTemplate;
    private final WebSocketProperties properties;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ConcurrentHashMap<String, ChannelReference> activeChannels = new ConcurrentHashMap<>();

    public void update(UUID workspaceId, UUID channelId, UUID userId, boolean typing) {
        String key = key(workspaceId, channelId);
        long now = Instant.now().toEpochMilli();
        if (typing) {
            redisTemplate.opsForZSet().add(key, userId.toString(), now + properties.typingTtlSeconds() * 1000);
            activeChannels.put(key, new ChannelReference(workspaceId, channelId));
        } else {
            redisTemplate.opsForZSet().remove(key, userId.toString());
        }
        publishSnapshot(key, workspaceId, channelId, now);
    }

    // TTL이 지난 입력 중 상태를 제거하고 남은 snapshot을 채널에 다시 알린다.
    @Scheduled(fixedDelay = 1000)
    public void expireTypingStates() {
        long now = Instant.now().toEpochMilli();
        activeChannels.forEach((key, reference) -> {
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
            if (removed != null && removed > 0) {
                publishSnapshot(key, reference.workspaceId(), reference.channelId(), now);
            }
        });
    }

    private void publishSnapshot(String key, UUID workspaceId, UUID channelId, long now) {
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
        List<UUID> userIds = redisTemplate.opsForZSet().range(key, 0, -1).stream()
                .map(UUID::fromString)
                .toList();
        if (userIds.isEmpty()) {
            activeChannels.remove(key);
            redisTemplate.delete(key);
        }
        realtimeEventPublisher.publishTyping(workspaceId, channelId, userIds);
    }

    private String key(UUID workspaceId, UUID channelId) {
        return KEY_PREFIX + workspaceId + ':' + channelId;
    }

    private record ChannelReference(UUID workspaceId, UUID channelId) {
    }
}
