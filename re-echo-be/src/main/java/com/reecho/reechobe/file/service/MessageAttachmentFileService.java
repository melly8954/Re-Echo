package com.reecho.reechobe.file.service;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.dto.MessageAttachmentPresignRequest;
import com.reecho.reechobe.file.dto.PresignedDownloadResponse;
import com.reecho.reechobe.file.dto.PresignedUploadResponse;
import com.reecho.reechobe.file.exception.FileErrorCode;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.infra.storage.R2StorageProperties;
import com.reecho.reechobe.infra.storage.StorageClient;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.message.repository.MessageAttachmentRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 메시지 첨부의 R2 업로드·다운로드 권한과 임시 파일 메타데이터를 관리한다.
@Service
@RequiredArgsConstructor
public class MessageAttachmentFileService {

    private static final long MESSAGE_ATTACHMENT_MAX_SIZE_BYTES = 20L * 1024L * 1024L;

    private final FileObjectRepository fileObjectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final StorageClient storageClient;
    private final R2StorageProperties r2StorageProperties;

    @Transactional
    // 쓰기 가능한 워크스페이스 멤버에게만 메시지 첨부의 직접 업로드 URL을 발급한다.
    public PresignedUploadResponse createUploadUrl(
            UUID userId,
            UUID workspaceId,
            MessageAttachmentPresignRequest request
    ) {
        if (request.size() > MESSAGE_ATTACHMENT_MAX_SIZE_BYTES) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
        WorkspaceMembership membership = requireWritableWorkspaceMembership(userId, workspaceId);
        UUID fileId = UUID.randomUUID();
        String storageKey = buildStorageKey(workspaceId, fileId, request.fileName());
        StorageClient.PresignedUpload presignedUpload = storageClient.presignPut(
                storageKey,
                request.contentType(),
                presignTtl()
        );
        FileObject fileObject = FileObject.createMessageAttachment(
                fileId,
                workspaceId,
                userId,
                membership.getId(),
                storageKey,
                request.fileName().trim(),
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

    // 메시지 트랜잭션 전에 R2 업로드 완료 여부를 확인해 DB 연결 점유를 피한다.
    public void requireUploadedAttachments(UUID userId, UUID workspaceId, List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        WorkspaceMembership membership = requireWritableWorkspaceMembership(userId, workspaceId);
        Map<UUID, FileObject> files = fileObjectRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileObject::getId, Function.identity()));
        if (files.size() != fileIds.size()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
        boolean invalidFile = files.values().stream().anyMatch(fileObject ->
                !workspaceId.equals(fileObject.getWorkspaceId())
                        || !userId.equals(fileObject.getUploadedByUserId())
                        || !membership.getId().equals(fileObject.getUploadedByMembershipId())
                        || !fileObject.isMessageAttachment()
                        || !fileObject.isActive()
        );
        if (invalidFile) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        boolean incompleteUpload = files.values().stream()
                .anyMatch(fileObject -> !storageClient.exists(fileObject.getStorageKey()));
        if (incompleteUpload) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        boolean invalidSize = files.values().stream().anyMatch(fileObject -> storageClient
                .findObjectSize(fileObject.getStorageKey())
                .map(actualSize -> actualSize != fileObject.getFileSizeBytes()
                        || actualSize > MESSAGE_ATTACHMENT_MAX_SIZE_BYTES)
                .orElse(true));
        if (invalidSize) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    // 현재 멤버가 볼 수 있는 메시지에 연결된 첨부 파일에만 다운로드 URL을 발급한다.
    public PresignedDownloadResponse createDownloadUrl(UUID userId, UUID workspaceId, UUID fileId) {
        WorkspaceMembership membership = requireAccessibleWorkspaceMembership(userId, workspaceId);
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        if (!workspaceId.equals(fileObject.getWorkspaceId())
                || !fileObject.isMessageAttachment()
                || !fileObject.isActive()
                || !messageAttachmentRepository.existsAccessibleMessageAttachment(fileId, membership.getId())) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        if (!storageClient.exists(fileObject.getStorageKey())) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        StorageClient.PresignedDownload presignedDownload = storageClient.presignGet(
                fileObject.getStorageKey(),
                presignTtl()
        );
        return new PresignedDownloadResponse(
                presignedDownload.downloadUrl(),
                presignedDownload.expiresAt()
        );
    }

    // 보관·삭제 워크스페이스에는 새 첨부를 올리지 못하도록 쓰기 가능 상태를 확인한다.
    private WorkspaceMembership requireWritableWorkspaceMembership(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
        return requireAccessibleWorkspaceMembership(userId, workspaceId);
    }

    // 탈퇴하거나 제거된 사용자가 파일 URL을 발급받지 못하도록 활성 멤버십만 허용한다.
    private WorkspaceMembership requireAccessibleWorkspaceMembership(UUID userId, UUID workspaceId) {
        workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(workspaceId, userId, WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    // 첨부 파일을 워크스페이스별 경로와 난수 식별자로 분리해 저장한다.
    private String buildStorageKey(UUID workspaceId, UUID fileId, String fileName) {
        return "workspaces/%s/attachments/%s/%s".formatted(
                workspaceId,
                fileId,
                normalizeStorageFileName(fileName)
        );
    }

    // 사용자 파일명이 저장소 키의 경로나 URL 규칙에 영향을 주지 않게 정규화한다.
    private String normalizeStorageFileName(String fileName) {
        String normalizedFileName = fileName.trim()
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalizedFileName.isBlank()) {
            return "attachment";
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
