package com.reecho.reechobe.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 새 워크스페이스 생성에 필요한 기본 정보를 받는다.
public record CreateWorkspaceRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        String imageUrl
) {
}
