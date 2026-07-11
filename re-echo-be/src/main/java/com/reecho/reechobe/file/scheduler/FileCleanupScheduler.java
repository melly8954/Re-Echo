package com.reecho.reechobe.file.scheduler;

import com.reecho.reechobe.file.config.FileCleanupProperties;
import com.reecho.reechobe.file.service.FileCleanupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 사용자 요청과 분리해 오래된 미연결 파일을 주기적으로 정리한다.
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private final FileCleanupService fileCleanupService;
    private final FileCleanupProperties fileCleanupProperties;

    @Scheduled(cron = "${reecho.file-cleanup.cron:0 0 3 * * *}")
    public void cleanupOrphanedFiles() {
        if (!fileCleanupProperties.enabled()) {
            return;
        }
        int deletedCount = fileCleanupService.cleanupOrphanedProfileImages();
        if (deletedCount > 0) {
            log.info("Deleted orphaned profile images. count={}", deletedCount);
        }
    }
}
