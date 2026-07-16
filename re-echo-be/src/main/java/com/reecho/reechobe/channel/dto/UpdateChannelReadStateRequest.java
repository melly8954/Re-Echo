package com.reecho.reechobe.channel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// 채널에서 마지막으로 확인한 메시지 위치를 전달한다.
public record UpdateChannelReadStateRequest(
        @NotNull UUID lastReadMessageId
) {
}
