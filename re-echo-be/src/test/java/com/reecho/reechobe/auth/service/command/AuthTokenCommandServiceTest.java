package com.reecho.reechobe.auth.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.jwt.JwtIssueService;
import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.auth.jwt.VerifiedToken;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.User;
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
class AuthTokenCommandServiceTest {

    @Mock
    private JwtVerifyService jwtVerifyService;

    @Mock
    private JwtIssueService jwtIssueService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private AuthTokenCommandService service;

    @BeforeEach
    void setUp() {
        service = new AuthTokenCommandService(
                jwtVerifyService,
                jwtIssueService,
                userRepository,
                refreshTokenStore
        );
    }

    @Test
    void refresh_token을_검증하고_새_토큰으로_회전한다() {
        UUID userId = UUID.randomUUID();
        UUID currentTokenId = UUID.randomUUID();
        User user = User.createActive();
        ReflectionTestUtils.setField(user, "id", userId);
        VerifiedToken verifiedToken = verifiedToken(userId, currentTokenId);
        AuthToken newToken = authToken();
        when(jwtVerifyService.verifyRefreshToken("refresh-token")).thenReturn(verifiedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtIssueService.issue(user)).thenReturn(newToken);
        when(refreshTokenStore.rotate(
                userId,
                currentTokenId,
                newToken.refreshTokenId(),
                newToken.refreshTokenExpiresAt()
        )).thenReturn(true);

        AuthToken result = service.refresh("refresh-token");

        assertThat(result).isSameAs(newToken);
        verify(refreshTokenStore).rotate(
                userId,
                currentTokenId,
                newToken.refreshTokenId(),
                newToken.refreshTokenExpiresAt()
        );
    }

    @Test
    void 이미_회전된_refresh_token이면_예외를_던진다() {
        UUID userId = UUID.randomUUID();
        UUID currentTokenId = UUID.randomUUID();
        User user = User.createActive();
        ReflectionTestUtils.setField(user, "id", userId);
        VerifiedToken verifiedToken = verifiedToken(userId, currentTokenId);
        AuthToken newToken = authToken();
        when(jwtVerifyService.verifyRefreshToken("replayed-token")).thenReturn(verifiedToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtIssueService.issue(user)).thenReturn(newToken);
        when(refreshTokenStore.rotate(
                userId,
                currentTokenId,
                newToken.refreshTokenId(),
                newToken.refreshTokenExpiresAt()
        )).thenReturn(false);

        assertThatThrownBy(() -> service.refresh("replayed-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    @Test
    void refresh_token으로_현재_세션을_폐기한다() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        VerifiedToken token = verifiedToken(userId, tokenId);
        when(jwtVerifyService.verifyRefreshToken("refresh-token")).thenReturn(token);
        when(refreshTokenStore.revoke(userId, tokenId)).thenReturn(true);

        service.logout(null, "refresh-token");

        verify(refreshTokenStore).revoke(userId, tokenId);
    }

    @Test
    void access_token_인증만_있어도_클라이언트_로그아웃을_허용한다() {
        service.logout(UUID.randomUUID(), null);

        verifyNoInteractions(jwtVerifyService, refreshTokenStore);
    }

    @Test
    void 인증_수단이_없으면_예외를_던진다() {
        assertThatThrownBy(() -> service.logout(null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    void redis에_없는_refresh_token만_제공하면_예외를_던진다() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        VerifiedToken token = verifiedToken(userId, tokenId);
        when(jwtVerifyService.verifyRefreshToken("revoked-token")).thenReturn(token);
        when(refreshTokenStore.revoke(userId, tokenId)).thenReturn(false);

        assertThatThrownBy(() -> service.logout(null, "revoked-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);
    }

    private VerifiedToken verifiedToken(UUID userId, UUID tokenId) {
        return new VerifiedToken(
                userId,
                tokenId,
                Instant.now().plusSeconds(60)
        );
    }

    private AuthToken authToken() {
        Instant now = Instant.now();
        return new AuthToken(
                "new-access-token",
                now.plusSeconds(3600),
                "new-refresh-token",
                UUID.randomUUID(),
                now.plusSeconds(1209600)
        );
    }
}
