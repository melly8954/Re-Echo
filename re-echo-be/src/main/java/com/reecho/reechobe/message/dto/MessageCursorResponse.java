package com.reecho.reechobe.message.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// 메시지 목록에서 다음 페이지 조회 기준이 되는 cursor 값을 표현한다.
public record MessageCursorResponse(
        LocalDateTime createdAt,
        UUID messageId
) {
}
