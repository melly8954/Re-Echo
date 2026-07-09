package com.reecho.reechobe.file.dto;

import java.time.Instant;
import java.util.UUID;

// 클라이언트가 R2로 직접 업로드할 때 필요한 값을 반환한다.
public record PresignedUploadResponse(
        UUID fileId,
        String uploadUrl,
        Instant expiresAt
) {
}
