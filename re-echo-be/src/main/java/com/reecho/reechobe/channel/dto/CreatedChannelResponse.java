package com.reecho.reechobe.channel.dto;

import java.util.UUID;

// 생성 직후 프런트가 해당 채널로 이동할 수 있도록 식별자를 반환한다.
public record CreatedChannelResponse(UUID id) {
}
