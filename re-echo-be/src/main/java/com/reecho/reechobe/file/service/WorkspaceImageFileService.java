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

// 워크스페이스 대표 이미지를 R2 파일 객체와 워크스페이스 사이에 연결한다.
@Service
@RequiredArgsConstructor
public class WorkspaceImageFileService {

    private static final long WORKSPACE_IMAGE_MAX_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final FileObjectRepository fileObjectRepository;
    private final StorageClient storageClient;
    private final R2StorageProperties r2StorageProperties;

    @Transactional
    // 워크스페이스 멤버가 변경할 대표 이미지의 직접 업로드 URL과 임시 메타데이터를 만든다.
    public PresignedUploadResponse createWorkspaceImageUpload(
            UUID workspaceId,
            UUID userId,
            UUID membershipId,
            ProfileImagePresignRequest request
    ) {
        validateWorkspaceImage(request);
        UUID fileId = UUID.randomUUID();
        String storageKey = buildWorkspaceImageStorageKey(workspaceId, fileId, request.fileName());
        StorageClient.PresignedUpload presignedUpload = storageClient.presignPut(
                storageKey,
                request.contentType(),
                presignTtl()
        );
        FileObject fileObject = FileObject.createWorkspaceImage(
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
    public String requireUploadedWorkspaceImageUrl(UUID workspaceId, UUID userId, UUID fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!fileObject.isOwnedBy(userId)
                || !fileObject.isWorkspaceImage()
                || !workspaceId.equals(fileObject.getWorkspaceId())
                || fileObject.isDeleted()) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        if (!storageClient.exists(fileObject.getStorageKey())) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        return storageClient.publicUrl(fileObject.getStorageKey());
    }

    @Transactional
    // 워크스페이스에 더 이상 연결되지 않은 대표 이미지를 정리 대상으로 표시한다.
    public void markWorkspaceImageOrphaned(UUID workspaceId, UUID fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!fileObject.isWorkspaceImage() || !workspaceId.equals(fileObject.getWorkspaceId())) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        fileObject.markOrphaned();
    }

    // 대표 이미지가 허용된 형식과 용량을 초과하지 않게 검증한다.
    private void validateWorkspaceImage(ProfileImagePresignRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())) {
            throw new BusinessException(FileErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED);
        }
        if (request.size() > WORKSPACE_IMAGE_MAX_SIZE_BYTES) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    // 워크스페이스 범위와 난수 식별자로 대표 이미지 객체 키를 구성한다.
    private String buildWorkspaceImageStorageKey(UUID workspaceId, UUID fileId, String fileName) {
        return "workspaces/%s/%s/%s".formatted(
                workspaceId,
                fileId,
                normalizeFileName(fileName)
        );
    }

    // 저장소 키가 사용자 입력 파일명에 따라 깨지지 않도록 허용 문자만 남긴다.
    private String normalizeFileName(String fileName) {
        String normalizedFileName = fileName.trim()
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalizedFileName.isBlank()) {
            return "workspace-image";
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
