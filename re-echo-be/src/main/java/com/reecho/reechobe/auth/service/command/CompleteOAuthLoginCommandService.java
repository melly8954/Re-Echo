package com.reecho.reechobe.auth.service.command;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.OAuthProvider;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.domain.UserIdentity;
import com.reecho.reechobe.user.repository.UserIdentityRepository;
import com.reecho.reechobe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// OAuth 공급자 식별자를 기준으로 내부 사용자 계정 연결을 완료한다.
@Service
@RequiredArgsConstructor
public class CompleteOAuthLoginCommandService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    // OAuth 공급자 식별자로 기존 사용자 또는 신규 연결 사용자를 반환한다.
    @Transactional
    public User complete(
            OAuthProvider provider,
            String providerUserId,
            String providerEmail
    ) {
        if (provider == null || providerUserId == null || providerUserId.isBlank()) {
            throw new BusinessException(AuthErrorCode.AUTH_OAUTH_AUTHENTICATION_FAILED);
        }

        return userIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserIdentity::getUser)
                .orElseGet(() -> createUserIdentity(provider, providerUserId, providerEmail));
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
