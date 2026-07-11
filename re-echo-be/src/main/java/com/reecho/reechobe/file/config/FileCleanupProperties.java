package com.reecho.reechobe.file.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 파일 정리 스케줄러의 실행 여부와 보관 유예 시간을 제어한다.
@ConfigurationProperties(prefix = "reecho.file-cleanup")
public record FileCleanupProperties(
        Boolean enabled,
        Duration orphanRetention,
        int batchSize
) {

    public FileCleanupProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (orphanRetention == null) {
            orphanRetention = Duration.ofHours(24);
        }
        if (batchSize <= 0) {
            batchSize = 100;
        }
    }
}
