package com.reecho.reechobe.auth.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.jwt.JwtIssueService;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.OAuthProvider;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.domain.UserIdentity;
import com.reecho.reechobe.user.repository.UserIdentityRepository;
import com.reecho.reechobe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// OAuth 사용자 연결부터 토큰 발급까지 로그인 유스케이스를 처리한다.
@Service
@RequiredArgsConstructor
public class OAuthLoginCommandService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final JwtIssueService jwtIssueService;
    private final RefreshTokenStore refreshTokenStore;

    // OAuth 사용자를 연결하고 Redis에 Refresh Token 상태를 저장한다.
    @Transactional
    public AuthToken login(
            OAuthProvider provider,
            String providerUserId,
            String providerEmail
    ) {
        if (provider == null || providerUserId == null || providerUserId.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_OAUTH_AUTHENTICATION_FAILED);
        }

        User user = userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getUser)
                .orElseGet(() -> createUserIdentity(provider, providerUserId, providerEmail));
        AuthToken token = jwtIssueService.issue(user);
        refreshTokenStore.save(
                user.getId(),
                token.refreshTokenId(),
                token.refreshTokenExpiresAt()
        );
        return token;
    }

    private User createUserIdentity(
            OAuthProvider provider,
            String providerUserId,
            String providerEmail
    ) {
        User user = userRepository.save(User.createActive());
        UserIdentity userIdentity = UserIdentity.createOAuthLink(user, provider, providerUserId, providerEmail);
        userIdentityRepository.save(userIdentity);
        return user;
    }
}
