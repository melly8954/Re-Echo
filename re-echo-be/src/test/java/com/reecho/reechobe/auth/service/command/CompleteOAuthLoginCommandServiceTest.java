package com.reecho.reechobe.auth.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.OAuthProvider;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.domain.UserIdentity;
import com.reecho.reechobe.user.repository.UserIdentityRepository;
import com.reecho.reechobe.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompleteOAuthLoginCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Test
    void 기존_식별자가_있으면_연결된_사용자를_반환한다() {
        CompleteOAuthLoginCommandService service = new CompleteOAuthLoginCommandService(
                userRepository,
                userIdentityRepository
        );
        User user = User.createActive();
        UserIdentity userIdentity = UserIdentity.createOAuthLink(
                user,
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@example.com"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).thenReturn(Optional.of(userIdentity));

        User result = service.complete(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@example.com"
        );

        assertThat(result).isSameAs(user);
        verify(userRepository, never()).save(any());
        verify(userIdentityRepository, never()).save(any());
    }

    @Test
    void 기존_식별자가_없으면_사용자와_식별자를_생성한다() {
        CompleteOAuthLoginCommandService service = new CompleteOAuthLoginCommandService(
                userRepository,
                userIdentityRepository
        );

        when(userIdentityRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "kakao-user-id"
        )).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.complete(
                OAuthProvider.KAKAO,
                "kakao-user-id",
                "user@example.com"
        );

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(userIdentityRepository).save(argThat(userIdentity ->
                userIdentity.getUser() == result
                        && userIdentity.getProvider() == OAuthProvider.KAKAO
                        && userIdentity.getProviderUserId().equals("kakao-user-id")
                        && userIdentity.getProviderEmail().equals("user@example.com")
        ));
    }

    @Test
    void 공급자_사용자_식별자가_비어있으면_인증_예외를_던진다() {
        CompleteOAuthLoginCommandService service = new CompleteOAuthLoginCommandService(
                userRepository,
                userIdentityRepository
        );

        assertThatThrownBy(() -> service.complete(OAuthProvider.GITHUB, " ", null))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any());
        verify(userIdentityRepository, never()).save(any());
    }
}
