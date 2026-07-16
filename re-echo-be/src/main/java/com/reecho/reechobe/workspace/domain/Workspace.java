package com.reecho.reechobe.workspace.domain;

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

// 최상위 협업 공간의 기본 정보와 보관 상태를 보존한다.
@Getter
@Entity
@Table(name = "workspaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Workspace {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_file_id")
    private UUID imageFileId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

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

    public static Workspace create(
            String name,
            String description,
            String imageUrl,
            UUID createdByUserId
    ) {
        return Workspace.builder()
                .id(UUID.randomUUID())
                .name(normalizeName(name))
                .description(normalizeNullable(description))
                .imageUrl(normalizeNullable(imageUrl))
                .createdByUserId(createdByUserId)
                .status(WorkspaceStatus.ACTIVE)
                .build();
    }

    // 대표 이미지 교체 전에는 서비스가 파일 소유권과 업로드 완료를 검증한다.
    public void update(String name, String description, UUID imageFileId, String imageUrl) {
        this.name = normalizeName(name);
        this.description = normalizeNullable(description);
        this.imageFileId = imageFileId;
        this.imageUrl = normalizeNullable(imageUrl);
    }

    // 보관 시각과 만료 시각을 함께 저장해 복원 가능 기간을 명확히 한다.
    public void archive(LocalDateTime archivedAt, LocalDateTime archiveExpiresAt) {
        this.status = WorkspaceStatus.ARCHIVED;
        this.archivedAt = archivedAt;
        this.archiveExpiresAt = archiveExpiresAt;
    }

    // 복원된 워크스페이스는 다음 보관 전까지 이전 보관 이력을 유지하지 않는다.
    public void restore() {
        this.status = WorkspaceStatus.ACTIVE;
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
        if (normalizedName.length() > 100) {
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
