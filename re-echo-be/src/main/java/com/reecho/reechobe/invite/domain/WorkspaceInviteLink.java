package com.reecho.reechobe.invite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// 워크스페이스 참여에 사용할 단기 초대 토큰을 보존한다.
@Getter
@Entity
@Table(name = "workspace_invite_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class WorkspaceInviteLink {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "token")
    private String token;

    @Column(name = "created_by_membership_id")
    private UUID createdByMembershipId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkspaceInviteLinkStatus status = WorkspaceInviteLinkStatus.ACTIVE;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static WorkspaceInviteLink create(
            UUID workspaceId,
            UUID createdByMembershipId,
            String token,
            LocalDateTime expiresAt
    ) {
        return WorkspaceInviteLink.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .createdByMembershipId(createdByMembershipId)
                .token(token)
                .status(WorkspaceInviteLinkStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(LocalDateTime now) {
        this.status = WorkspaceInviteLinkStatus.REVOKED;
        this.revokedAt = now;
    }

    public void expire() {
        this.status = WorkspaceInviteLinkStatus.EXPIRED;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
