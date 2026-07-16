package com.reecho.reechobe.realtime.dto;

import com.reecho.reechobe.message.dto.ChannelMessageResponse;

// 메시지 변경 이벤트가 화면을 즉시 갱신할 수 있도록 전체 snapshot을 담는다.
public record MessageEventPayload(ChannelMessageResponse message) {
}
