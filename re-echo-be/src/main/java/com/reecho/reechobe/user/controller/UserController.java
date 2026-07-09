package com.reecho.reechobe.user.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import com.reecho.reechobe.user.dto.UpdateUserProfileRequest;
import com.reecho.reechobe.user.dto.UserResponse;
import com.reecho.reechobe.user.service.command.UserProfileCommandService;
import com.reecho.reechobe.user.service.query.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 사용자의 계정 기본 정보를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserQueryService userQueryService;
    private final UserProfileCommandService userProfileCommandService;

    @GetMapping
    public ApiResponse<UserResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        UserResponse result = userQueryService.getCurrentUser(principal.userId());
        return ApiResponse.success(HttpStatus.OK, "현재 계정 프로필을 조회합니다.", result);
    }

    @PatchMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserResponse result = userProfileCommandService.updateProfile(
                principal.userId(),
                request
        );
        return ApiResponse.success(HttpStatus.OK, "프로필이 수정되었습니다.", result);
    }
}
