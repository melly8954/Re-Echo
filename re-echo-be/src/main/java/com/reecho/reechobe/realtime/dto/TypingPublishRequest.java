package com.reecho.reechobe.realtime.dto;

// composer가 입력 중 시작·종료 상태를 채널에 전달한다.
public record TypingPublishRequest(boolean typing) {
}
