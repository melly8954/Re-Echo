package com.reecho.reechobe.file.domain;

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

// R2에 저장될 파일의 소유자와 연결 목적을 DB에 보존한다.
@Getter
@Entity
@Table(name = "file_objects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class FileObject {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;

    @Column(name = "uploaded_by_membership_id")
    private UUID uploadedByMembershipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    private FilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider")
    private StorageProvider storageProvider;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FileStatus status;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "orphaned_at")
    private LocalDateTime orphanedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static FileObject createProfileImage(
            UUID fileId,
            UUID uploadedByUserId,
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes
    ) {
        return FileObject.builder()
                .id(fileId)
                .uploadedByUserId(uploadedByUserId)
                .purpose(FilePurpose.PROFILE_IMAGE)
                .storageProvider(StorageProvider.R2)
                .storageKey(storageKey)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .fileSizeBytes(fileSizeBytes)
                .status(FileStatus.ACTIVE)
                .build();
    }

    public static FileObject createMessageAttachment(
            UUID fileId,
            UUID workspaceId,
            UUID uploadedByUserId,
            UUID uploadedByMembershipId,
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes
    ) {
        return FileObject.builder()
                .id(fileId)
                .workspaceId(workspaceId)
                .uploadedByUserId(uploadedByUserId)
                .uploadedByMembershipId(uploadedByMembershipId)
                .purpose(FilePurpose.MESSAGE_ATTACHMENT)
                .storageProvider(StorageProvider.R2)
                .storageKey(storageKey)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .fileSizeBytes(fileSizeBytes)
                .status(FileStatus.ACTIVE)
                .build();
    }

    public static FileObject createWorkspaceImage(
            UUID fileId,
            UUID workspaceId,
            UUID uploadedByUserId,
            UUID uploadedByMembershipId,
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes
    ) {
        return FileObject.builder()
                .id(fileId)
                .workspaceId(workspaceId)
                .uploadedByUserId(uploadedByUserId)
                .uploadedByMembershipId(uploadedByMembershipId)
                .purpose(FilePurpose.WORKSPACE_IMAGE)
                .storageProvider(StorageProvider.R2)
                .storageKey(storageKey)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .fileSizeBytes(fileSizeBytes)
                .status(FileStatus.ACTIVE)
                .build();
    }

    public boolean isOwnedBy(UUID userId) {
        return uploadedByUserId.equals(userId);
    }

    public boolean isProfileImage() {
        return purpose == FilePurpose.PROFILE_IMAGE;
    }

    public boolean isMessageAttachment() {
        return purpose == FilePurpose.MESSAGE_ATTACHMENT;
    }

    public boolean isWorkspaceImage() {
        return purpose == FilePurpose.WORKSPACE_IMAGE;
    }

    public boolean isActive() {
        return status == FileStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == FileStatus.DELETED;
    }

    public boolean isOrphaned() {
        return status == FileStatus.ORPHANED;
    }

    public void markOrphaned() {
        if (isDeleted()) {
            return;
        }
        this.status = FileStatus.ORPHANED;
        this.orphanedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.status = FileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.uploadedAt = now;
        this.createdAt = now;
    }
}
