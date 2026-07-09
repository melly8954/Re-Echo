package com.reecho.reechobe.auth.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
class RefreshTokenCommandServiceTest {

    @Mock
    private JwtVerifyService jwtVerifyService;

    @Mock
    private JwtIssueService jwtIssueService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private RefreshTokenCommandService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenCommandService(
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
        VerifiedToken verifiedToken = new VerifiedToken(
                userId,
                currentTokenId,
                Instant.now().plusSeconds(60)
        );
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
        VerifiedToken verifiedToken = new VerifiedToken(
                userId,
                currentTokenId,
                Instant.now().plusSeconds(60)
        );
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
