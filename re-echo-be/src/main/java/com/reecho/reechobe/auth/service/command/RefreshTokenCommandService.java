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

// Refresh Token을 검증하고 새 Access/Refresh Token 쌍을 발급한다.
@Service
@RequiredArgsConstructor
public class RefreshTokenCommandService {

    private final JwtVerifyService jwtVerifyService;
    private final JwtIssueService jwtIssueService;
    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;

    // 쿠키로 받은 Refresh Token이 유효한 사용자에게만 새 토큰을 발급한다.
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
}
