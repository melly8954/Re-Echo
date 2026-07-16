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
    // 계정 소유자에게만 허용 확장자와 용량을 검증한 프로필 이미지 업로드 URL을 발급한다.
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

    // 외부 스토리지 확인 중 DB 커넥션 점유를 피하기 위해 트랜잭션을 열지 않는다.
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

    @Transactional
    // 더 이상 프로필에 연결되지 않은 계정 소유자의 이미지 파일을 정리 대상으로 표시한다.
    public void markAccountProfileImageOrphaned(UUID userId, UUID fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!fileObject.isOwnedBy(userId) || !fileObject.isProfileImage()) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        fileObject.markOrphaned();
    }

    // 브라우저가 선언한 파일 형식과 크기를 서버 정책 범위로 제한한다.
    private void validateProfileImage(ProfileImagePresignRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
            throw new BusinessException(FileErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED);
        }
        if (request.size() > PROFILE_IMAGE_MAX_SIZE_BYTES) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    // 계정과 난수 파일 식별자를 포함해 서로 충돌하지 않는 R2 경로를 만든다.
    private String buildProfileImageStorageKey(UUID userId, UUID fileId, String fileName) {
        return "profiles/%s/%s/%s".formatted(
                userId,
                fileId,
                normalizeFileName(fileName)
        );
    }

    // 객체 키에 사용할 수 없는 파일명 문자를 치환해 경로 해석 문제를 막는다.
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
