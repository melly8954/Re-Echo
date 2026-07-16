package com.reecho.reechobe.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.domain.FileStatus;
import com.reecho.reechobe.file.dto.PresignedUploadResponse;
import com.reecho.reechobe.file.dto.ProfileImagePresignRequest;
import com.reecho.reechobe.file.exception.FileErrorCode;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.infra.storage.R2StorageProperties;
import com.reecho.reechobe.infra.storage.StorageClient;
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
class ProfileImageFileServiceTest {

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private StorageClient storageClient;

    private ProfileImageFileService service;

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
        service = new ProfileImageFileService(fileObjectRepository, storageClient, properties);
    }

    @Test
    void 계정_프로필_이미지_업로드_URL을_발급한다() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-10T12:10:00Z");
        when(storageClient.presignPut(any(String.class), eq("image/png"), eq(Duration.ofMinutes(10))))
                .thenReturn(new StorageClient.PresignedUpload("https://r2-presigned-url", expiresAt));
        when(fileObjectRepository.save(any(FileObject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PresignedUploadResponse response = service.createAccountProfileImageUpload(
                userId,
                new ProfileImagePresignRequest("profile.png", "image/png", 1200L)
        );

        assertThat(response.fileId()).isNotNull();
        assertThat(response.uploadUrl()).isEqualTo("https://r2-presigned-url");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void 워크스페이스_프로필_이미지_업로드_URL을_발급한다() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-07-10T12:10:00Z");
        when(storageClient.presignPut(any(String.class), eq("image/png"), eq(Duration.ofMinutes(10))))
                .thenReturn(new StorageClient.PresignedUpload("https://r2-presigned-url", expiresAt));
        when(fileObjectRepository.save(any(FileObject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PresignedUploadResponse response = service.createWorkspaceProfileImageUpload(
                workspaceId,
                userId,
                membershipId,
                new ProfileImagePresignRequest("profile.png", "image/png", 1200L)
        );

        assertThat(response.fileId()).isNotNull();
        assertThat(response.uploadUrl()).isEqualTo("https://r2-presigned-url");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void 업로드가_완료된_본인_프로필_이미지_URL을_반환한다() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        FileObject fileObject = FileObject.createProfileImage(
                fileId,
                userId,
                "profiles/user/file/profile.png",
                "profile.png",
                "image/png",
                1200L
        );
        when(fileObjectRepository.findById(fileId)).thenReturn(Optional.of(fileObject));
        when(storageClient.exists(fileObject.getStorageKey())).thenReturn(true);
        when(storageClient.publicUrl(fileObject.getStorageKey()))
                .thenReturn("https://cdn.example.com/profiles/user/file/profile.png");

        String profileImageUrl = service.requireUploadedAccountProfileImageUrl(userId, fileId);

        assertThat(profileImageUrl)
                .isEqualTo("https://cdn.example.com/profiles/user/file/profile.png");
    }

    @Test
    void 허용되지_않은_프로필_이미지_형식이면_예외가_발생한다() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.createAccountProfileImageUpload(
                userId,
                new ProfileImagePresignRequest("profile.gif", "image/gif", 1200L)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FileErrorCode.FILE_CONTENT_TYPE_NOT_ALLOWED.getDefaultMessage());
    }

    @Test
    void 본인_프로필_이미지를_고아_파일로_표시한다() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        FileObject fileObject = FileObject.createProfileImage(
                fileId,
                userId,
                "profiles/user/file/profile.png",
                "profile.png",
                "image/png",
                1200L
        );
        when(fileObjectRepository.findById(fileId)).thenReturn(Optional.of(fileObject));

        service.markAccountProfileImageOrphaned(userId, fileId);

        assertThat(fileObject.getStatus()).isEqualTo(FileStatus.ORPHANED);
        assertThat(fileObject.getOrphanedAt()).isNotNull();
    }
}
