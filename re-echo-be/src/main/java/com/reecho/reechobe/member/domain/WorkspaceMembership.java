package com.reecho.reechobe.member.domain;

import com.reecho.reechobe.user.domain.User;
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

// 워크스페이스 권한과 워크스페이스별 프로필 기준을 보존한다.
@Getter
@Entity
@Table(name = "workspace_memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class WorkspaceMembership {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private WorkspaceMembershipRole role;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "profile_image_file_id")
    private UUID profileImageFileId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkspaceMembershipStatus status = WorkspaceMembershipStatus.ACTIVE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static WorkspaceMembership createOwner(UUID workspaceId, User user) {
        return WorkspaceMembership.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(user.getId())
                .role(WorkspaceMembershipRole.OWNER)
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .profileImageFileId(user.getProfileImageFileId())
                .status(WorkspaceMembershipStatus.ACTIVE)
                .build();
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
