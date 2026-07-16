package com.reecho.reechobe.message.dto;

import java.util.List;
import java.util.UUID;

// 메시지 수정 시 본문과 첨부 파일 전체 목록을 교체한다.
public record UpdateMessageRequest(
        String content,
        List<UUID> fileIds
) {
}
