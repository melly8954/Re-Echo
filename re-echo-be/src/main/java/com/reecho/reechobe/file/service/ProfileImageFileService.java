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

    @Transactional
    // 워크스페이스 멤버 본인의 프로필에만 연결할 업로드 URL을 발급한다.
    public PresignedUploadResponse createWorkspaceProfileImageUpload(
            UUID workspaceId,
            UUID userId,
            UUID membershipId,
            ProfileImagePresignRequest request
    ) {
        validateProfileImage(request);
        UUID fileId = UUID.randomUUID();
        String storageKey = buildWorkspaceProfileImageStorageKey(
                workspaceId,
                membershipId,
                fileId,
                request.fileName()
        );
        StorageClient.PresignedUpload presignedUpload = storageClient.presignPut(
                storageKey,
                request.contentType(),
                presignTtl()
        );
        FileObject fileObject = FileObject.createWorkspaceProfileImage(
                fileId,
                workspaceId,
                userId,
                membershipId,
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
        FileObject fileObject = fileObjectRepository.findByIdForUpdate(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!fileObject.isOwnedBy(userId) || !fileObject.isProfileImage() || fileObject.isDeleted()) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        if (!storageClient.exists(fileObject.getStorageKey())) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        requireMatchingImageSize(fileObject);
        return storageClient.publicUrl(fileObject.getStorageKey());
    }

    // 업로드한 이미지가 현재 워크스페이스 멤버십에 속하는지 함께 검증한다.
    public String requireUploadedWorkspaceProfileImageUrl(
            UUID workspaceId,
            UUID userId,
            UUID membershipId,
            UUID fileId
    ) {
        FileObject fileObject = fileObjectRepository.findByIdForUpdate(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!fileObject.isOwnedBy(userId)
                || !fileObject.isProfileImage()
                || !workspaceId.equals(fileObject.getWorkspaceId())
                || !membershipId.equals(fileObject.getUploadedByMembershipId())
                || fileObject.isDeleted()) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        if (!storageClient.exists(fileObject.getStorageKey())) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        requireMatchingImageSize(fileObject);
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

    @Transactional
    // 워크스페이스 전용 이미지일 때만 이전 파일을 정리 대상으로 표시한다.
    public void markWorkspaceProfileImageOrphaned(
            UUID workspaceId,
            UUID membershipId,
            UUID fileId
    ) {
        fileObjectRepository.findById(fileId)
                .filter(FileObject::isProfileImage)
                .filter(fileObject -> workspaceId.equals(fileObject.getWorkspaceId()))
                .filter(fileObject -> membershipId.equals(fileObject.getUploadedByMembershipId()))
                .ifPresent(FileObject::markOrphaned);
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

    // 클라이언트가 선언한 메타데이터와 실제 업로드 객체 크기가 같아야 연결한다.
    private void requireMatchingImageSize(FileObject fileObject) {
        long actualSize = storageClient.findObjectSize(fileObject.getStorageKey())
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED));
        if (actualSize != fileObject.getFileSizeBytes() || actualSize > PROFILE_IMAGE_MAX_SIZE_BYTES) {
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

    // 워크스페이스와 멤버십을 키에 포함해 다른 프로필 문맥의 재사용을 막는다.
    private String buildWorkspaceProfileImageStorageKey(
            UUID workspaceId,
            UUID membershipId,
            UUID fileId,
            String fileName
    ) {
        return "workspaces/%s/members/%s/profiles/%s/%s".formatted(
                workspaceId,
                membershipId,
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
