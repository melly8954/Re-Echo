package com.reecho.reechobe.realtime.dto;

import java.util.List;
import java.util.UUID;

// 채널에서 현재 입력 중인 멤버 식별자를 snapshot으로 전달한다.
public record TypingEventPayload(List<UUID> typingUserIds) {
}
