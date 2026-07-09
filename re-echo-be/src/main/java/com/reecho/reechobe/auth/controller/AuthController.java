package com.reecho.reechobe.auth.controller;

import com.reecho.reechobe.auth.dto.RefreshTokenResponse;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.handler.RefreshTokenCookieWriter;
import com.reecho.reechobe.auth.service.command.AuthTokenCommandService;
import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 토큰 재발급과 로그아웃 API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthTokenCommandService authTokenCommandService;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    // Refresh Token 쿠키를 검증하고 Access Token은 응답 body로 반환한다.
    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        AuthToken token = authTokenCommandService.refresh(refreshToken);
        refreshTokenCookieWriter.add(response, token);
        RefreshTokenResponse result = new RefreshTokenResponse(
                token.accessToken(),
                token.accessTokenExpiresAt()
        );
        return ApiResponse.success(HttpStatus.OK, "토큰이 재발급되었습니다.", result);
    }

    // Access Token 또는 Refresh Token으로 현재 로그인 세션을 종료한다.
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authTokenCommandService.logout(
                principal == null ? null : principal.userId(),
                refreshToken
        );
        refreshTokenCookieWriter.expire(response);
        return ApiResponse.success(HttpStatus.OK, "로그아웃되었습니다.", null);
    }
}
