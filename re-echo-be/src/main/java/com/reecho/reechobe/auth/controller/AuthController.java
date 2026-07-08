package com.reecho.reechobe.auth.controller;

import com.reecho.reechobe.auth.dto.RefreshTokenResponse;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.handler.RefreshTokenCookieWriter;
import com.reecho.reechobe.auth.service.AuthToken;
import com.reecho.reechobe.auth.service.OAuth2LoginProperties;
import com.reecho.reechobe.auth.service.command.RefreshTokenCommandService;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 인증 토큰 재발급 API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RefreshTokenCommandService refreshTokenCommandService;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;
    private final OAuth2LoginProperties properties;

    // Refresh Token 쿠키를 검증하고 Access Token은 응답 body로 반환한다.
    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthToken token = refreshTokenCommandService.refresh(refreshToken(request));
        refreshTokenCookieWriter.add(response, token);
        RefreshTokenResponse result = new RefreshTokenResponse(
                token.accessToken(),
                token.accessTokenExpiresAt()
        );
        return ApiResponse.success(HttpStatus.OK, "토큰이 재발급되었습니다.", result);
    }

    private String refreshToken(HttpServletRequest request) {
        if (properties.getRefreshTokenCookieName() == null || properties.getRefreshTokenCookieName().isBlank()) {
            throw new IllegalStateException("Refresh Token 쿠키 이름 설정은 필수입니다.");
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        return Arrays.stream(cookies)
                .filter(cookie -> properties.getRefreshTokenCookieName().equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }
}
