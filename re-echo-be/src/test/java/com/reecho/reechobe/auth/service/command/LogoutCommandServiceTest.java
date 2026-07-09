package com.reecho.reechobe.auth.service.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.auth.jwt.VerifiedToken;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.common.exception.BusinessException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutCommandServiceTest {

    @Mock
    private JwtVerifyService jwtVerifyService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private LogoutCommandService service;

    @BeforeEach
    void setUp() {
        service = new LogoutCommandService(jwtVerifyService, refreshTokenStore);
    }

    @Test
    void refresh_token으로_현재_세션을_폐기한다() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        VerifiedToken token = new VerifiedToken(
                userId,
                tokenId,
                Instant.now().plusSeconds(60)
        );
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
        VerifiedToken token = new VerifiedToken(
                userId,
                tokenId,
                Instant.now().plusSeconds(60)
        );
        when(jwtVerifyService.verifyRefreshToken("revoked-token")).thenReturn(token);
        when(refreshTokenStore.revoke(userId, tokenId)).thenReturn(false);

        assertThatThrownBy(() -> service.logout(null, "revoked-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.AUTH_UNAUTHORIZED);
    }
}
