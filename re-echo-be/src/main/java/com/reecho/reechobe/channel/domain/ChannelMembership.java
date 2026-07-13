package com.reecho.reechobe.channel.domain;

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

// 워크스페이스 멤버십 기준의 채널 참여 상태를 보존한다.
@Getter
@Entity
@Table(name = "channel_memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ChannelMembership {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "workspace_membership_id")
    private UUID workspaceMembershipId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChannelMembershipStatus status = ChannelMembershipStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ChannelMembership join(UUID channelId, UUID workspaceMembershipId) {
        return ChannelMembership.builder()
                .id(UUID.randomUUID())
                .channelId(channelId)
                .workspaceMembershipId(workspaceMembershipId)
                .status(ChannelMembershipStatus.ACTIVE)
                .build();
    }

    public void rejoin() {
        this.status = ChannelMembershipStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
    }

    public void leave() {
        this.status = ChannelMembershipStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
