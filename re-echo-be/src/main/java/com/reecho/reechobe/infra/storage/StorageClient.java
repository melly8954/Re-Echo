package com.reecho.reechobe.infra.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// 애플리케이션 코드가 특정 스토리지 SDK에 직접 의존하지 않게 한다.
public interface StorageClient {

    PresignedUpload presignPut(String storageKey, String contentType, Duration ttl);

    PresignedDownload presignGet(String storageKey, Duration ttl);

    boolean exists(String storageKey);

    Optional<Long> findObjectSize(String storageKey);

    void delete(String storageKey);

    String publicUrl(String storageKey);

    record PresignedUpload(
            String uploadUrl,
            Instant expiresAt
    ) {
    }

    record PresignedDownload(
            String downloadUrl,
            Instant expiresAt
    ) {
    }
}
