package com.reecho.reechobe.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 메시지와 이미 업로드된 파일의 표시 순서를 함께 보존한다.
@Getter
@Entity
@Table(name = "message_attachments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MessageAttachment {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "file_object_id")
    private UUID fileObjectId;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static MessageAttachment create(UUID messageId, UUID fileObjectId, int sortOrder) {
        return MessageAttachment.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .fileObjectId(fileObjectId)
                .sortOrder(sortOrder)
                .build();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
