package com.reecho.reechobe.message.dto;

import com.reecho.reechobe.message.domain.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// REST 응답과 실시간 event에서 공통으로 사용하는 전체 메시지 snapshot이다.
public record ChannelMessageResponse(
        UUID id,
        UUID channelId,
        MessageAuthorResponse author,
        String content,
        List<MessageAttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean edited,
        boolean deleted,
        boolean moderatorDeleted
) {

    public static ChannelMessageResponse of(
            Message message,
            MessageAuthorResponse author,
            List<MessageAttachmentResponse> attachments
    ) {
        return new ChannelMessageResponse(
                message.getId(),
                message.getChannelId(),
                author,
                message.getContent(),
                attachments,
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.getEditedAt() != null,
                message.isDeleted(),
                message.isModeratorDeleted()
        );
    }
}
