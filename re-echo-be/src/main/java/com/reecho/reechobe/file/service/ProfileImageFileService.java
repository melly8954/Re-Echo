package com.reecho.reechobe.file.service;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.dto.PresignedUploadResponse;
import com.reecho.reechobe.file.dto.ProfileImagePresignRequest;
import com.reecho.reechobe.file.exception.FileErrorCode;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.infra.storage.R2StorageProperties;
import com.reecho.reechobe.infra.storage.StorageClient;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 프로필 이미지를 R2 파일 객체와 사용자 프로필 사이에 연결한다.
@Service
@RequiredArgsConstructor
public class ProfileImageFileService {

    private static final long PROFILE_IMAGE_MAX_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final FileObjectRepository fileObjectRepository;
    private final StorageClient storageClient;
    private final R2StorageProperties r2StorageProperties;

    @Transactional
    public PresignedUploadResponse createAccountProfileImageUpload(
            UUID userId,
            ProfileImagePresignRequest request
    ) {
        validateProfileImage(request);
        UUID fileId = UUID.randomUUID();
        String storageKey = buildProfileImageStorageKey(userId, fileId, request.fileName());
        Duration ttl = presignTtl();
        StorageClient.PresignedUpload presignedUpload = storageClient.presignPut(
                storageKey,
                request.contentType(),
                ttl
        );
        FileObject fileObject = FileObject.createProfileImage(
                fileId,
                userId,
                storageKey,
                normalizeFileName(request.fileName()),
                request.contentType(),
                request.size()
        );
        fileObjectRepository.save(fileObject);
        return new PresignedUploadResponse(
                fileObject.getId(),
                presignedUpload.uploadUrl(),
                presignedUpload.expiresAt()
        );
    }

    @Transactional(readOnly = true)
    public String requireUploadedAccountProfileImageUrl(UUID userId, UUID fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!fileObject.isOwnedBy(userId) || !fileObject.isProfileImage() || fileObject.isDeleted()) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        if (!storageClient.exists(fileObject.getStorageKey())) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        return storageClient.publicUrl(fileObject.getStorageKey());
    }

    private void validateProfileImage(ProfileImagePresignRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
            throw new BusinessException(FileErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED);
        }
        if (request.size() > PROFILE_IMAGE_MAX_SIZE_BYTES) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    private String buildProfileImageStorageKey(UUID userId, UUID fileId, String fileName) {
        return "profiles/%s/%s/%s".formatted(
                userId,
                fileId,
                normalizeFileName(fileName)
        );
    }

    private String normalizeFileName(String fileName) {
        String normalizedFileName = fileName.trim()
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalizedFileName.isBlank()) {
            return "profile-image";
        }
        return normalizedFileName;
    }

    private Duration presignTtl() {
        if (r2StorageProperties.presignTtl() == null) {
            return Duration.ofMinutes(10);
        }
        return r2StorageProperties.presignTtl();
    }
}
