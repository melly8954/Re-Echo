package com.reecho.reechobe.auth.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.service.AuthToken;
import com.reecho.reechobe.auth.service.JwtIssueService;
import com.reecho.reechobe.auth.service.JwtVerifyService;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.repository.UserRepository;
import java.util.UUID;
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

    // 쿠키로 받은 Refresh Token이 유효한 사용자에게만 새 토큰을 발급한다.
    @Transactional(readOnly = true)
    public AuthToken refresh(String refreshToken) {
        UUID userId = jwtVerifyService.verifyRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        return jwtIssueService.issue(user);
    }
}
