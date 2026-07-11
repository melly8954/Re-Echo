package com.reecho.reechobe.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import com.reecho.reechobe.auth.jwt.AuthToken;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class RefreshTokenCookieWriterTest {

    @Test
    void refresh_token을_http_only_쿠키로_추가한다() {
        RefreshTokenCookieWriter writer = new RefreshTokenCookieWriter(properties());
        MockHttpServletResponse response = new MockHttpServletResponse();
        Instant now = Instant.now();
        AuthToken token = new AuthToken(
                "access-token",
                now.plusSeconds(3600),
                "refresh-token",
                UUID.randomUUID(),
                now.plusSeconds(1209600)
        );

        writer.add(response, token);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .contains("refreshToken=refresh-token")
                .contains("Path=/")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void 로그아웃하면_refresh_token_쿠키를_즉시_만료시킨다() {
        RefreshTokenCookieWriter writer = new RefreshTokenCookieWriter(properties());
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.expire(response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .contains("refreshToken=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }

    private OAuth2LoginProperties properties() {
        OAuth2LoginProperties properties = new OAuth2LoginProperties();
        properties.setRefreshTokenCookieName("refreshToken");
        properties.setRefreshTokenSecure(false);
        return properties;
    }
}
