package com.reecho.reechobe.file.dto;

import java.time.Instant;

// 클라이언트가 R2에서 파일을 조회할 때 필요한 값을 반환한다.
public record PresignedDownloadResponse(
        String downloadUrl,
        Instant expiresAt
) {
}
