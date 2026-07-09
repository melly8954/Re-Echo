package com.reecho.reechobe.auth.controller;

import com.reecho.reechobe.auth.dto.RefreshTokenResponse;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.service.command.RefreshTokenCommandService;
import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 토큰 재발급 API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RefreshTokenCommandService refreshTokenCommandService;

    // Refresh Token 쿠키를 검증하고 Access Token은 응답 body로 반환한다.
    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        AuthToken token = refreshTokenCommandService.refresh(refreshToken);
        RefreshTokenResponse result = new RefreshTokenResponse(
                token.accessToken(),
                token.accessTokenExpiresAt()
        );
        return ApiResponse.success(HttpStatus.OK, "토큰이 재발급되었습니다.", result);
    }
}
