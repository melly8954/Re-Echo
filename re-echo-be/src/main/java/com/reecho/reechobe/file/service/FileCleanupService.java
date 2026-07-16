package com.reecho.reechobe.file.service;

import com.reecho.reechobe.file.config.FileCleanupProperties;
import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.infra.storage.StorageClient;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 최종 연결되지 않은 이미지와 메시지 첨부를 외부 스토리지와 DB에서 정리한다.
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final FileObjectRepository fileObjectRepository;
    private final StorageClient storageClient;
    private final FileCleanupProperties fileCleanupProperties;

    @Transactional
    // 프로필과 워크스페이스에서 더 이상 참조하지 않는 오래된 이미지 파일을 정리한다.
    public int cleanupOrphanedProfileImages() {
        LocalDateTime cutoff = LocalDateTime.now().minus(fileCleanupProperties.orphanRetention());
        List<FileObject> candidates = fileObjectRepository.findProfileImageCleanupCandidates(
                cutoff,
                PageRequest.of(0, fileCleanupProperties.batchSize())
        );
        int deletedCount = 0;
        for (FileObject candidate : candidates) {
            if (deleteIfStillUnreferenced(candidate.getId())) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Transactional
    // 메시지에 연결되지 않은 오래된 첨부 파일을 정리한다.
    public int cleanupUnattachedMessageAttachments() {
        LocalDateTime cutoff = LocalDateTime.now().minus(fileCleanupProperties.orphanRetention());
        List<FileObject> candidates = fileObjectRepository.findUnattachedMessageCleanupCandidates(
                cutoff,
                PageRequest.of(0, fileCleanupProperties.batchSize())
        );
        int deletedCount = 0;
        for (FileObject candidate : candidates) {
            if (deleteMessageAttachmentIfStillUnreferenced(candidate.getId())) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    // 조회 이후 다시 연결된 파일을 삭제하지 않도록 외부 객체 삭제 직전에 참조를 재확인한다.
    private boolean deleteIfStillUnreferenced(java.util.UUID fileId) {
        FileObject fileObject = fileObjectRepository.findByIdForUpdate(fileId)
                .filter(candidate -> !candidate.isDeleted())
                .orElse(null);
        if (fileObject == null) {
            return false;
        }
        if (fileObjectRepository.existsProfileImageReference(fileObject.getId())) {
            return false;
        }
        try {
            storageClient.delete(fileObject.getStorageKey());
            fileObject.markDeleted();
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "고아 이미지 삭제에 실패했습니다. fileObjectId={}, storageKey={}",
                    fileObject.getId(),
                    fileObject.getStorageKey(),
                    exception
            );
            return false;
        }
    }

    // 비동기 메시지 작성과 경합해도 연결된 첨부 파일을 지우지 않도록 다시 확인한다.
    private boolean deleteMessageAttachmentIfStillUnreferenced(java.util.UUID fileId) {
        FileObject fileObject = fileObjectRepository.findByIdForUpdate(fileId)
                .filter(candidate -> !candidate.isDeleted())
                .orElse(null);
        if (fileObject == null) {
            return false;
        }
        if (fileObjectRepository.existsMessageAttachmentReference(fileObject.getId())) {
            return false;
        }
        try {
            storageClient.delete(fileObject.getStorageKey());
            fileObject.markDeleted();
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "고아 첨부 파일 삭제에 실패했습니다. fileObjectId={}, storageKey={}",
                    fileObject.getId(),
                    fileObject.getStorageKey(),
                    exception
            );
            return false;
        }
    }
}
