package com.reecho.reechobe.auth.handler;

import com.reecho.reechobe.auth.jwt.AuthToken;
import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import com.reecho.reechobe.auth.oauth.OAuth2UserProfile;
import com.reecho.reechobe.auth.oauth.OAuth2UserProfileExtractor;
import com.reecho.reechobe.auth.service.command.CompleteOAuthLoginCommandService;
import com.reecho.reechobe.auth.service.command.IssueAuthTokenCommandService;
import com.reecho.reechobe.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

// OAuth 로그인 성공 후 내부 사용자 연결과 Re-Echo 토큰 발급을 처리한다.
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2UserProfileExtractor profileExtractor;
    private final CompleteOAuthLoginCommandService completeOAuthLoginCommandService;
    private final IssueAuthTokenCommandService issueAuthTokenCommandService;
    private final OAuth2LoginProperties properties;
    private final RefreshTokenCookieWriter refreshTokenCookieWriter;

    // OAuth2 사용자 정보를 내부 사용자로 연결하고 프론트엔드 완료 URL로 이동시킨다.
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        validateProperties();

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2UserProfile profile = profileExtractor.extract(oauthToken);
        User user = completeOAuthLoginCommandService.complete(
                profile.provider(),
                profile.providerUserId(),
                profile.providerEmail()
        );
        AuthToken token = issueAuthTokenCommandService.issue(user);

        refreshTokenCookieWriter.add(response, token);
        invalidateOAuthSession(request);
        response.sendRedirect(properties.getSuccessRedirectUri());
    }

    private void validateProperties() {
        if (properties.getRefreshTokenCookieName() == null || properties.getRefreshTokenCookieName().isBlank()) {
            throw new IllegalStateException("Refresh Token 쿠키 이름 설정은 필수입니다.");
        }

        if (properties.getSuccessRedirectUri() == null || properties.getSuccessRedirectUri().isBlank()) {
            throw new IllegalStateException("OAuth 로그인 성공 리다이렉트 URI 설정은 필수입니다.");
        }
    }

    private void invalidateOAuthSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
