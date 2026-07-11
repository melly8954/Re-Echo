package com.reecho.reechobe.user.dto;

import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.domain.UserStatus;
import java.util.UUID;

// 인증 사용자의 계정 기본 프로필을 반환한다.
public record UserResponse(
        UUID id,
        String displayName,
        String profileImageUrl,
        UserStatus status
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfileImageUrl(),
                user.getStatus()
        );
    }
}
