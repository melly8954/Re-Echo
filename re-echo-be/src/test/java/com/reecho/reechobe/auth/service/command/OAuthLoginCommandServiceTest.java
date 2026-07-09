package com.reecho.reechobe.auth.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.jwt.JwtIssueService;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.OAuthProvider;
import com.reecho.reechobe.user.domain.User;
import com.reecho.reechobe.user.domain.UserIdentity;
import com.reecho.reechobe.user.repository.UserIdentityRepository;
import com.reecho.reechobe.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuthLoginCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private JwtIssueService jwtIssueService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private OAuthLoginCommandService service;

    @BeforeEach
    void setUp() {
        service = new OAuthLoginCommandService(
                userRepository,
                userIdentityRepository,
                jwtIssueService,
                refreshTokenStore
        );
    }

    @Test
    void 기존_oauth_사용자에게_토큰을_발급하고_refresh_token을_저장한다() {
        UUID userId = UUID.randomUUID();
        User user = User.createActive("기존 사용자", null);
        ReflectionTestUtils.setField(user, "id", userId);
        UserIdentity userIdentity = UserIdentity.createOAuthLink(
                user,
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@example.com"
        );
        AuthToken token = authToken();
        when(userIdentityRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-id"
        )).thenReturn(Optional.of(userIdentity));
        when(jwtIssueService.issue(user)).thenReturn(token);

        AuthToken result = service.login(
                OAuthProvider.GOOGLE,
                "google-user-id",
                "user@example.com",
                "변경된 이름",
                "https://example.com/new-profile.png"
        );

        assertThat(result).isSameAs(token);
        verify(userRepository, never()).save(any());
        verify(userIdentityRepository, never()).save(any());
        verify(refreshTokenStore).save(
                userId,
                token.refreshTokenId(),
                token.refreshTokenExpiresAt()
        );
    }

    @Test
    void 신규_oauth_사용자를_연결하고_토큰을_발급한다() {
        UUID userId = UUID.randomUUID();
        AuthToken token = authToken();
        when(userIdentityRepository.findByProviderAndProviderUserId(
                OAuthProvider.KAKAO,
                "kakao-user-id"
        )).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", userId);
            return user;
        });
        when(jwtIssueService.issue(any(User.class))).thenReturn(token);

        AuthToken result = service.login(
                OAuthProvider.KAKAO,
                "kakao-user-id",
                "user@example.com",
                "카카오 사용자",
                "https://example.com/profile.png"
        );

        assertThat(result).isSameAs(token);
        verify(userRepository).save(argThat(user ->
                user.getDisplayName().equals("카카오 사용자")
                        && user.getProfileImageUrl().equals("https://example.com/profile.png")
        ));
        verify(userIdentityRepository).save(argThat(userIdentity ->
                userIdentity.getProvider() == OAuthProvider.KAKAO
                        && userIdentity.getProviderUserId().equals("kakao-user-id")
                        && userIdentity.getProviderEmail().equals("user@example.com")
        ));
        verify(refreshTokenStore).save(
                userId,
                token.refreshTokenId(),
                token.refreshTokenExpiresAt()
        );
    }

    @Test
    void 공급자_사용자_식별자가_비어있으면_인증_예외를_던진다() {
        assertThatThrownBy(() -> service.login(
                OAuthProvider.GITHUB,
                " ",
                null,
                "사용자",
                null
        ))
                .isInstanceOf(BusinessException.class);

        verify(userRepository, never()).save(any());
        verify(userIdentityRepository, never()).save(any());
        verifyNoInteractions(jwtIssueService, refreshTokenStore);
    }

    private AuthToken authToken() {
        Instant now = Instant.now();
        return new AuthToken(
                "access-token",
                now.plusSeconds(3600),
                "refresh-token",
                UUID.randomUUID(),
                now.plusSeconds(1209600)
        );
    }
}
