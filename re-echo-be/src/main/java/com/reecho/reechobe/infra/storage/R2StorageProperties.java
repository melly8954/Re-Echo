package com.reecho.reechobe.infra.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// R2 연동에 필요한 환경 설정을 한 곳에서 바인딩한다.
@ConfigurationProperties(prefix = "reecho.storage.r2")
public record R2StorageProperties(
        String endpoint,
        String bucket,
        String accessKey,
        String secretKey,
        String publicBaseUrl,
        Duration presignTtl
) {
}
