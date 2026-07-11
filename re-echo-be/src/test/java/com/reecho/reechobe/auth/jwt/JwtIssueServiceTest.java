package com.reecho.reechobe.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reecho.reechobe.auth.config.JwtCodecConfig;
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
        JwtIssueService service = jwtIssueService(properties);
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        AuthToken token = service.issue(user);

        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.refreshToken()).isNotBlank();
        assertThat(token.refreshTokenId()).isNotNull();
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
        JwtIssueService service = jwtIssueService(properties);

        assertThatThrownBy(() -> service.issue(User.createActive("사용자", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("토큰을 발급할 사용자 식별자는 필수입니다.");
    }

    @Test
    void 필수_설정이_없으면_예외를_던진다() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-change-me-test-secret-change-me");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        JwtIssueService service = jwtIssueService(properties);
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        assertThatThrownBy(() -> service.issue(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT 발급자 설정은 필수입니다.");
    }

    @Test
    void access_token_ttl이_0이면_예외를_던진다() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("re-echo");
        properties.setSecret("test-secret-change-me-test-secret-change-me");
        properties.setAccessTokenTtl(Duration.ZERO);
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        JwtIssueService service = jwtIssueService(properties);
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        assertThatThrownBy(() -> service.issue(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Access Token 만료 시간 설정은 0보다 커야 합니다.");
    }

    @Test
    void refresh_token_ttl이_음수이면_예외를_던진다() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("re-echo");
        properties.setSecret("test-secret-change-me-test-secret-change-me");
        properties.setAccessTokenTtl(Duration.ofMinutes(30));
        properties.setRefreshTokenTtl(Duration.ofSeconds(-1));
        JwtIssueService service = jwtIssueService(properties);
        User user = User.createActive("사용자", null);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        assertThatThrownBy(() -> service.issue(user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Refresh Token 만료 시간 설정은 0보다 커야 합니다.");
    }

    private JwtIssueService jwtIssueService(JwtProperties properties) {
        JwtCodecConfig codecConfig = new JwtCodecConfig();
        return new JwtIssueService(
                properties,
                codecConfig.jwtEncoder(codecConfig.jwtSecretKey(properties))
        );
    }
}
