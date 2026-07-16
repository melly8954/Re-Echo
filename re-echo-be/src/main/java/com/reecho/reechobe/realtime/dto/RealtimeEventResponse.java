package com.reecho.reechobe.realtime.dto;

import java.time.Instant;
import java.util.UUID;

// 클라이언트 중복 제거와 최신성 판단에 사용하는 공통 event envelope다.
public record RealtimeEventResponse<T>(
        UUID eventId,
        RealtimeEventType type,
        Instant occurredAt,
        T payload
) {

    public static <T> RealtimeEventResponse<T> of(RealtimeEventType type, T payload) {
        return new RealtimeEventResponse<>(UUID.randomUUID(), type, Instant.now(), payload);
    }
}
