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

    // 토큰 상태와 관계없이 로그아웃 상태를 보장하고 유효한 세션만 폐기한다.
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        VerifiedToken verifiedToken;
        try {
            verifiedToken = jwtVerifyService.verifyRefreshToken(refreshToken);
        } catch (BusinessException exception) {
            // 만료·무효 토큰은 이미 로그아웃된 상태로 간주한다.
            return;
        }

        refreshTokenStore.revoke(
                verifiedToken.userId(),
                verifiedToken.tokenId()
        );
    }
}
