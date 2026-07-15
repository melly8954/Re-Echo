package com.reecho.reechobe.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

// 워크스페이스 기본 정보와 대표 이미지 변경값을 받는다.
public record UpdateWorkspaceRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        UUID imageFileId
) {
}
