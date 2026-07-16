package com.reecho.reechobe.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 메시지 첨부 파일의 업로드 URL 발급에 필요한 메타데이터를 전달한다.
public record MessageAttachmentPresignRequest(
        @NotBlank
        String fileName,
        @NotBlank
        String contentType,
        @NotNull
        @Positive
        Long size
) {
}
