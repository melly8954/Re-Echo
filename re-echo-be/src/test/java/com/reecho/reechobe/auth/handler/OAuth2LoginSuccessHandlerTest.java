package com.reecho.reechobe.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.oauth.OAuth2UserProfile;
import com.reecho.reechobe.auth.oauth.OAuth2UserProfileExtractor;
import com.reecho.reechobe.auth.service.command.OAuthLoginCommandService;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.user.domain.OAuthProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private OAuth2UserProfileExtractor profileExtractor;

    @Mock
    private OAuthLoginCommandService oauthLoginCommandService;

    @Mock
    private RefreshTokenCookieWriter refreshTokenCookieWriter;

    private OAuth2LoginProperties properties;
    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        properties = new OAuth2LoginProperties();
        properties.setRefreshTokenCookieName("refreshToken");
        properties.setSuccessRedirectUri("http://localhost:5173/oauth/callback");
        properties.setFailureRedirectUri("http://localhost:5173/login");
        OAuth2LoginFailureHandler failureHandler = new OAuth2LoginFailureHandler(properties);
        handler = new OAuth2LoginSuccessHandler(
                profileExtractor,
                oauthLoginCommandService,
                properties,
                refreshTokenCookieWriter,
                failureHandler
        );
    }

    @Test
    void oauth_로그인에_성공하면_토큰_쿠키를_추가하고_완료_화면으로_이동한다() throws Exception {
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        OAuth2UserProfile profile = new OAuth2UserProfile(
                OAuthProvider.GOOGLE,
                "provider-user-id",
                "user@example.com"
        );
        AuthToken token = authToken();
        when(profileExtractor.extract(authentication)).thenReturn(profile);
        when(oauthLoginCommandService.login(
                profile.provider(),
                profile.providerUserId(),
                profile.providerEmail()
        )).thenReturn(token);
        MockHttpServletRequest request = requestWithSession();
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(refreshTokenCookieWriter).add(response, token);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/oauth/callback");
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void oauth_로그인_후처리에_실패하면_공통_오류_코드로_로그인_화면에_이동한다() throws Exception {
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(profileExtractor.extract(authentication))
                .thenThrow(new BusinessException(AuthErrorCode.AUTH_OAUTH_AUTHENTICATION_FAILED));
        MockHttpServletRequest request = requestWithSession();
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo(
                        "http://localhost:5173/login"
                                + "?errorCode=AUTH_OAUTH_AUTHENTICATION_FAILED"
                );
        assertThat(session.isInvalid()).isTrue();
        verifyNoInteractions(oauthLoginCommandService, refreshTokenCookieWriter);
    }

    private MockHttpServletRequest requestWithSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/login/oauth2/code/google");
        request.setSession(new MockHttpSession());
        return request;
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
