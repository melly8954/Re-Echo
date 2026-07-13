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

    @Column(name = "last_visited_at")
    private LocalDateTime lastVisitedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

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

    public static WorkspaceMembership createMember(UUID workspaceId, User user) {
        return WorkspaceMembership.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(user.getId())
                .role(WorkspaceMembershipRole.MEMBER)
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .profileImageFileId(user.getProfileImageFileId())
                .status(WorkspaceMembershipStatus.ACTIVE)
                .build();
    }

    // 재참여 시 현재 계정 기본 프로필을 새 멤버십 초기값처럼 다시 복사한다.
    public void rejoinAsMember(User user) {
        this.role = WorkspaceMembershipRole.MEMBER;
        this.displayName = user.getDisplayName();
        this.profileImageUrl = user.getProfileImageUrl();
        this.profileImageFileId = user.getProfileImageFileId();
        this.status = WorkspaceMembershipStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
        this.lastVisitedAt = this.joinedAt;
        this.leftAt = null;
    }

    // 워크스페이스 재진입 순서와 복귀 기준을 위한 방문 시각을 갱신한다.
    public void visit() {
        this.lastVisitedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.lastVisitedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
