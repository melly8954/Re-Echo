package com.reecho.reechobe.auth.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.auth.jwt.VerifiedToken;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.common.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Access Token 또는 Refresh Token을 기준으로 현재 세션을 종료한다.
@Service
@RequiredArgsConstructor
public class LogoutCommandService {

    private final JwtVerifyService jwtVerifyService;
    private final RefreshTokenStore refreshTokenStore;

    // Refresh Token이 있으면 해당 세션을 폐기하고 Access 인증만 있어도 로그아웃을 허용한다.
    public void logout(UUID authenticatedUserId, String refreshToken) {
        boolean accessAuthenticated = authenticatedUserId != null;
        if (refreshToken == null || refreshToken.isBlank()) {
            if (!accessAuthenticated) {
                throw new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED);
            }
            return;
        }

        VerifiedToken verifiedToken;
        try {
            verifiedToken = jwtVerifyService.verifyRefreshToken(refreshToken);
        } catch (BusinessException exception) {
            if (!accessAuthenticated) {
                throw new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED);
            }
            return;
        }

        if (accessAuthenticated && !authenticatedUserId.equals(verifiedToken.userId())) {
            throw new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED);
        }

        boolean revoked = refreshTokenStore.revoke(
                verifiedToken.userId(),
                verifiedToken.tokenId()
        );
        if (!revoked && !accessAuthenticated) {
            throw new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED);
        }
    }
}
