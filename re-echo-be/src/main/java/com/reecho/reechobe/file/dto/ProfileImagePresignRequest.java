package com.reecho.reechobe.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 프로필 이미지 업로드 URL 발급에 필요한 파일 정보를 전달한다.
public record ProfileImagePresignRequest(
        @NotBlank
        String fileName,
        @NotBlank
        String contentType,
        @NotNull
        @Positive
        Long size
) {
}
