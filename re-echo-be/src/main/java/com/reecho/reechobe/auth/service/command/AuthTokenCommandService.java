package com.reecho.reechobe.auth.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.jwt.JwtIssueService;
import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.auth.jwt.VerifiedToken;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Refresh Token 재발급과 로그아웃을 포함한 토큰 생명주기를 관리한다.
@Service
@RequiredArgsConstructor
public class AuthTokenCommandService {

    private final JwtVerifyService jwtVerifyService;
    private final JwtIssueService jwtIssueService;
    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;

    // 유효한 Refresh Token을 새 Access/Refresh Token 쌍으로 회전한다.
    @Transactional(readOnly = true)
    public AuthToken refresh(String refreshToken) {
        VerifiedToken verifiedToken = jwtVerifyService.verifyRefreshToken(refreshToken);
        User user = userRepository.findById(verifiedToken.userId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        AuthToken newToken = jwtIssueService.issue(user);
        boolean rotated = refreshTokenStore.rotate(
                verifiedToken.userId(),
                verifiedToken.tokenId(),
                newToken.refreshTokenId(),
                newToken.refreshTokenExpiresAt()
        );
        if (!rotated) {
            throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }
        return newToken;
    }

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
