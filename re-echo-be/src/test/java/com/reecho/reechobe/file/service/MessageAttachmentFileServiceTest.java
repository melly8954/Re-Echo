package com.reecho.reechobe.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.message.repository.MessageAttachmentRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageAttachmentFileServiceTest {

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Mock
    private MessageAttachmentRepository messageAttachmentRepository;

    @Mock
    private StorageClient storageClient;

    @Mock
    private Workspace workspace;

    @Mock
    private WorkspaceMembership membership;

    private MessageAttachmentFileService service;

    @BeforeEach
    void setUp() {
        R2StorageProperties properties = new R2StorageProperties(
                "https://example.r2.cloudflarestorage.com",
                "bucket",
                "access-key",
                "secret-key",
                "https://cdn.example.com",
                Duration.ofMinutes(10)
        );
        service = new MessageAttachmentFileService(
                fileObjectRepository,
                workspaceRepository,
                workspaceMembershipRepository,
                messageAttachmentRepository,
                storageClient,
                properties
        );
    }

    @Test
    void 워크스페이스_멤버에게_첨부_파일_업로드_URL을_발급한다() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-16T12:10:00Z");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspace.getStatus()).thenReturn(WorkspaceStatus.ACTIVE);
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(membership.getId()).thenReturn(membershipId);
        when(storageClient.presignPut(any(String.class), eq("application/pdf"), any(Duration.class)))
                .thenReturn(new StorageClient.PresignedUpload("https://r2-presigned-url", expiresAt));
        when(fileObjectRepository.save(any(FileObject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PresignedUploadResponse response = service.createUploadUrl(
                userId,
                workspaceId,
                new MessageAttachmentPresignRequest("meeting.pdf", "application/pdf", 1200L)
        );

        assertThat(response.fileId()).isNotNull();
        assertThat(response.uploadUrl()).isEqualTo("https://r2-presigned-url");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void 업로드되지_않은_첨부_파일은_메시지에_연결할_수_없다() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        FileObject fileObject = FileObject.createMessageAttachment(
                fileId,
                workspaceId,
                userId,
                membershipId,
                "workspaces/workspace/attachments/file/meeting.pdf",
                "meeting.pdf",
                "application/pdf",
                1200L
        );
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspace.getStatus()).thenReturn(WorkspaceStatus.ACTIVE);
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(membership.getId()).thenReturn(membershipId);
        when(fileObjectRepository.findAllById(any())).thenReturn(java.util.List.of(fileObject));
        when(storageClient.exists(fileObject.getStorageKey())).thenReturn(false);

        assertThatThrownBy(() -> service.requireUploadedAttachments(userId, workspaceId, java.util.List.of(fileId)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED.getDefaultMessage());
    }

    @Test
    void 접근_가능한_메시지_첨부의_다운로드_URL을_발급한다() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-16T12:10:00Z");
        FileObject fileObject = FileObject.createMessageAttachment(
                fileId,
                workspaceId,
                userId,
                membershipId,
                "workspaces/workspace/attachments/file/image.png",
                "image.png",
                "image/png",
                1200L
        );
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspace.getStatus()).thenReturn(WorkspaceStatus.ACTIVE);
        when(workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(membership));
        when(membership.getId()).thenReturn(membershipId);
        when(fileObjectRepository.findById(fileId)).thenReturn(Optional.of(fileObject));
        when(messageAttachmentRepository.existsAccessibleMessageAttachment(fileId, membershipId)).thenReturn(true);
        when(storageClient.exists(fileObject.getStorageKey())).thenReturn(true);
        when(storageClient.presignGet(fileObject.getStorageKey(), Duration.ofMinutes(10)))
                .thenReturn(new StorageClient.PresignedDownload("https://r2-presigned-url", expiresAt));

        PresignedDownloadResponse response = service.createDownloadUrl(userId, workspaceId, fileId);

        assertThat(response.downloadUrl()).isEqualTo("https://r2-presigned-url");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }
}
