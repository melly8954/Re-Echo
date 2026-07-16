package com.reecho.reechobe.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 채널 대화의 본문과 수정·삭제 이력을 보존한다.
@Getter
@Entity
@Table(name = "messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Message {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "author_membership_id")
    private UUID authorMembershipId;

    @Column(name = "content")
    private String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status = MessageStatus.ACTIVE;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_membership_id")
    private UUID deletedByMembershipId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Message create(UUID channelId, UUID authorMembershipId, String content) {
        return Message.builder()
                .id(UUID.randomUUID())
                .channelId(channelId)
                .authorMembershipId(authorMembershipId)
                .content(content)
                .status(MessageStatus.ACTIVE)
                .build();
    }

    public void edit(String content) {
        this.content = content;
        this.editedAt = LocalDateTime.now();
    }

    public void delete(UUID deletedByMembershipId) {
        this.status = MessageStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.deletedByMembershipId = deletedByMembershipId;
    }

    public boolean isDeleted() {
        return status == MessageStatus.DELETED;
    }

    // 작성자 외 관리 권한자의 삭제임을 응답에서 구분할 수 있게 한다.
    public boolean isModeratorDeleted() {
        return isDeleted()
                && deletedByMembershipId != null
                && !deletedByMembershipId.equals(authorMembershipId);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
