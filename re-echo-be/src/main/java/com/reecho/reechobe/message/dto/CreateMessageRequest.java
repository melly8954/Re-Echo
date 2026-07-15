package com.reecho.reechobe.message.dto;

import java.util.List;
import java.util.UUID;

// 메시지 본문과 이미 업로드된 첨부 파일 식별자를 함께 전달한다.
public record CreateMessageRequest(
        String content,
        List<UUID> fileIds
) {
}
