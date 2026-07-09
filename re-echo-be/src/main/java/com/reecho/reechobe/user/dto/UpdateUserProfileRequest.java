package com.reecho.reechobe.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 계정 기본 프로필 수정 값을 전달한다.
public record UpdateUserProfileRequest(
        @NotBlank
        @Size(max = 80)
        String displayName,
        String profileImageUrl
) {
}
