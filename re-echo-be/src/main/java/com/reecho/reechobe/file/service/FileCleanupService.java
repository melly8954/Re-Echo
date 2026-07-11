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

// 프로필에서 더 이상 참조하지 않는 파일을 외부 스토리지와 DB에서 정리한다.
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final FileObjectRepository fileObjectRepository;
    private final StorageClient storageClient;
    private final FileCleanupProperties fileCleanupProperties;

    @Transactional
    public int cleanupOrphanedProfileImages() {
        LocalDateTime cutoff = LocalDateTime.now().minus(fileCleanupProperties.orphanRetention());
        List<FileObject> candidates = fileObjectRepository.findProfileImageCleanupCandidates(
                cutoff,
                PageRequest.of(0, fileCleanupProperties.batchSize())
        );
        int deletedCount = 0;
        for (FileObject candidate : candidates) {
            if (deleteIfStillUnreferenced(candidate)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    private boolean deleteIfStillUnreferenced(FileObject fileObject) {
        if (fileObjectRepository.existsProfileImageReference(fileObject.getId())) {
            return false;
        }
        try {
            storageClient.delete(fileObject.getStorageKey());
            fileObject.markDeleted();
            return true;
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to delete orphaned file object. fileObjectId={}, storageKey={}",
                    fileObject.getId(),
                    fileObject.getStorageKey(),
                    exception
            );
            return false;
        }
    }
}
