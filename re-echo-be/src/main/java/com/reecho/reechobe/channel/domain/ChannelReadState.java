package com.reecho.reechobe.channel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// 채널 멤버십별 마지막 읽은 메시지 위치를 보존한다.
@Getter
@Entity
@Table(name = "channel_read_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ChannelReadState {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "channel_membership_id")
    private UUID channelMembershipId;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ChannelReadState create(UUID channelMembershipId, UUID lastReadMessageId) {
        return ChannelReadState.builder()
                .id(UUID.randomUUID())
                .channelMembershipId(channelMembershipId)
                .lastReadMessageId(lastReadMessageId)
                .lastReadAt(LocalDateTime.now())
                .build();
    }

    public void advanceTo(UUID lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = LocalDateTime.now();
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
