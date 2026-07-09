package com.reecho.reechobe.auth.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.jwt.JwtIssueService;
import com.reecho.reechobe.auth.refresh.RefreshTokenStore;
import com.reecho.reechobe.user.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IssueAuthTokenCommandServiceTest {

    @Mock
    private JwtIssueService jwtIssueService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Test
    void 로그인_토큰을_발급하고_refresh_token_상태를_저장한다() {
        IssueAuthTokenCommandService service = new IssueAuthTokenCommandService(
                jwtIssueService,
                refreshTokenStore
        );
        UUID userId = UUID.randomUUID();
        User user = User.createActive();
        ReflectionTestUtils.setField(user, "id", userId);
        AuthToken token = authToken();
        when(jwtIssueService.issue(user)).thenReturn(token);

        AuthToken result = service.issue(user);

        assertThat(result).isSameAs(token);
        verify(refreshTokenStore).save(
                userId,
                token.refreshTokenId(),
                token.refreshTokenExpiresAt()
        );
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
