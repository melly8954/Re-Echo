package com.reecho.reechobe.channel.domain;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.CommonErrorCode;
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

// 워크스페이스 내부 대화 공간과 기본 채널 여부를 보존한다.
@Getter
@Entity
@Table(name = "channels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Channel {

    public static final String GENERAL_CHANNEL_NAME = "general";

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private ChannelVisibility visibility;

    @Column(name = "is_general")
    private boolean general;

    @Column(name = "created_by_membership_id")
    private UUID createdByMembershipId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChannelStatus status = ChannelStatus.ACTIVE;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archive_expires_at")
    private LocalDateTime archiveExpiresAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Channel createGeneral(UUID workspaceId, UUID createdByMembershipId) {
        return Channel.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name(GENERAL_CHANNEL_NAME)
                .visibility(ChannelVisibility.PUBLIC)
                .general(true)
                .createdByMembershipId(createdByMembershipId)
                .status(ChannelStatus.ACTIVE)
                .build();
    }

    public static Channel create(
            UUID workspaceId,
            String name,
            String description,
            ChannelVisibility visibility,
            UUID createdByMembershipId
    ) {
        return Channel.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .name(normalizeName(name))
                .description(normalizeNullable(description))
                .visibility(visibility)
                .general(false)
                .createdByMembershipId(createdByMembershipId)
                .status(ChannelStatus.ACTIVE)
                .build();
    }

    // 관리자가 채널의 식별 정보와 소개 문구를 함께 갱신한다.
    public void update(String name, String description) {
        this.name = normalizeName(name);
        this.description = normalizeNullable(description);
    }

    // 워크스페이스 보관과 개별 보관 모두 같은 수명주기 상태를 사용한다.
    public void archive(LocalDateTime archivedAt, LocalDateTime archiveExpiresAt) {
        this.status = ChannelStatus.ARCHIVED;
        this.archivedAt = archivedAt;
        this.archiveExpiresAt = archiveExpiresAt;
    }

    // 보관을 유발한 워크스페이스가 복원될 때만 서비스가 이 상태를 되돌린다.
    public void restore() {
        this.status = ChannelStatus.ACTIVE;
        this.archivedAt = null;
        this.archiveExpiresAt = null;
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

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 80) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
        }
        return normalizedName;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
