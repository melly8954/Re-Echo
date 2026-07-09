package com.reecho.reechobe.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reecho.reechobe.auth.config.JwtProperties;
import com.reecho.reechobe.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtIssueServiceTest {

    @Test
    void 사용자_식별자를_기준으로_access_refresh_token을_발급한다() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("re-echo");
        properties.setSecret("test-secret-change-me-test-secret-change-me");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        JwtIssueService service = new JwtIssueService(properties);
        User user = User.createActive();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        AuthToken token = service.issue(user);

        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.refreshToken()).isNotBlank();
        assertThat(token.accessTokenExpiresAt()).isAfter(Instant.now());
        assertThat(token.refreshTokenExpiresAt()).isAfter(token.accessTokenExpiresAt());
    }

    @Test
    void 사용자_식별자가_없으면_예외를_던진다() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("re-echo");
        properties.setSecret("test-secret-change-me-test-secret-change-me");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        JwtIssueService service = new JwtIssueService(properties);

        assertThatThrownBy(() -> service.issue(User.createActive()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("토큰을 발급할 사용자 식별자는 필수입니다.");
    }

    @Test
    void 필수_설정이_없으면_예외를_던진다() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-change-me-test-secret-change-me");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        JwtIssueService service = new JwtIssueService(properties);
        User user = User.createActive();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        assertThatThrownBy(() -> service.issue(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT 발급자 설정은 필수입니다.");
    }
}
