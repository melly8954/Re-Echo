package com.reecho.reechobe.auth.service.command;

import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.jwt.JwtIssueService;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 로그인 사용자에게 토큰을 발급하고 Refresh Token 상태를 저장한다.
@Service
@RequiredArgsConstructor
public class IssueAuthTokenCommandService {

    private final JwtIssueService jwtIssueService;
    private final RefreshTokenStore refreshTokenStore;

    // 새 토큰 쌍을 발급하고 세션별 Refresh Token을 활성화한다.
    public AuthToken issue(User user) {
        AuthToken token = jwtIssueService.issue(user);
        refreshTokenStore.save(
                user.getId(),
                token.refreshTokenId(),
                token.refreshTokenExpiresAt()
        );
        return token;
    }
}
