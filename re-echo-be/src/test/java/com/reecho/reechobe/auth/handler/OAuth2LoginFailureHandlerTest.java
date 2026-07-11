package com.reecho.reechobe.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class OAuth2LoginFailureHandlerTest {

    @Test
    void oauth_인증_실패_원인을_노출하지_않고_로그인_화면으로_이동한다() throws Exception {
        OAuth2LoginProperties properties = properties();
        OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/login/oauth2/code/google");
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("provider_access_denied", "Provider 상세 오류", null)
        );

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        "http://localhost:5173/login"
                                + "?errorCode=AUTH_OAUTH_AUTHENTICATION_FAILED"
                )
                .doesNotContain("provider_access_denied", "Provider 상세 오류");
        assertThat(session.isInvalid()).isTrue();
    }

    private OAuth2LoginProperties properties() {
        OAuth2LoginProperties properties = new OAuth2LoginProperties();
        properties.setFailureRedirectUri("http://localhost:5173/login");
        return properties;
    }
}
