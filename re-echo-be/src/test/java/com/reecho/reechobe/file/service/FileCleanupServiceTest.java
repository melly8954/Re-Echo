package com.reecho.reechobe.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.file.config.FileCleanupProperties;
import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.domain.FileStatus;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.infra.storage.StorageClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private StorageClient storageClient;

    private FileCleanupService service;

    @BeforeEach
    void setUp() {
        FileCleanupProperties properties = new FileCleanupProperties(true, Duration.ofHours(24), 100);
        service = new FileCleanupService(fileObjectRepository, storageClient, properties);
    }

    @Test
    void 참조되지_않은_프로필_이미지를_R2와_DB에서_삭제한다() {
        UUID fileId = UUID.randomUUID();
        FileObject fileObject = FileObject.createProfileImage(
                fileId,
                UUID.randomUUID(),
                "profiles/user/file/profile.png",
                "profile.png",
                "image/png",
                1200L
        );
        fileObject.markOrphaned();
        when(fileObjectRepository.findProfileImageCleanupCandidates(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(fileObject));
        when(fileObjectRepository.existsProfileImageReference(fileId)).thenReturn(false);

        int deletedCount = service.cleanupOrphanedProfileImages();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(fileObject.getStatus()).isEqualTo(FileStatus.DELETED);
        assertThat(fileObject.getDeletedAt()).isNotNull();
        verify(storageClient).delete(fileObject.getStorageKey());
    }

    @Test
    void 삭제_직전_다시_참조된_파일은_삭제하지_않는다() {
        UUID fileId = UUID.randomUUID();
        FileObject fileObject = FileObject.createProfileImage(
                fileId,
                UUID.randomUUID(),
                "profiles/user/file/profile.png",
                "profile.png",
                "image/png",
                1200L
        );
        when(fileObjectRepository.findProfileImageCleanupCandidates(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(fileObject));
        when(fileObjectRepository.existsProfileImageReference(fileId)).thenReturn(true);

        int deletedCount = service.cleanupOrphanedProfileImages();

        assertThat(deletedCount).isZero();
        verify(storageClient, never()).delete(any());
    }

    @Test
    void 메시지에_연결되지_않은_첨부_파일을_R2와_DB에서_삭제한다() {
        UUID fileId = UUID.randomUUID();
        FileObject fileObject = FileObject.createMessageAttachment(
                fileId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "workspaces/workspace/attachments/file/meeting.pdf",
                "meeting.pdf",
                "application/pdf",
                1200L
        );
        when(fileObjectRepository.findUnattachedMessageCleanupCandidates(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(fileObject));
        when(fileObjectRepository.existsMessageAttachmentReference(fileId)).thenReturn(false);

        int deletedCount = service.cleanupUnattachedMessageAttachments();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(fileObject.getStatus()).isEqualTo(FileStatus.DELETED);
        verify(storageClient).delete(fileObject.getStorageKey());
    }
}
