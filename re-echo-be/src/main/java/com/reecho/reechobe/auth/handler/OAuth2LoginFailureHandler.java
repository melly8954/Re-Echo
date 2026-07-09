package com.reecho.reechobe.auth.handler;

import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

// OAuth 로그인 실패 시 프론트엔드 로그인 화면으로 이동시킨다.
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final OAuth2LoginProperties properties;

    // 실패 원인을 노출하지 않고 공통 실패 URL로 리다이렉트한다.
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.warn(
                "OAuth 인증에 실패했습니다. uri={}, exception={}",
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
        redirectFailure(request, response);
    }

    // 인증 성공 후 내부 처리 실패를 기록하고 같은 실패 화면으로 이동시킨다.
    void handlePostAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            RuntimeException exception
    ) throws IOException {
        log.error("OAuth 로그인 후처리에 실패했습니다. uri={}", request.getRequestURI(), exception);
        redirectFailure(request, response);
    }

    private void redirectFailure(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (properties.getFailureRedirectUri() == null || properties.getFailureRedirectUri().isBlank()) {
            throw new IllegalStateException("OAuth 로그인 실패 리다이렉트 URI 설정은 필수입니다.");
        }

        invalidateOAuthSession(request);
        String redirectUri = UriComponentsBuilder
                .fromUriString(properties.getFailureRedirectUri())
                .queryParam("errorCode", AuthErrorCode.AUTH_OAUTH_AUTHENTICATION_FAILED.name())
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUri);
    }

    private void invalidateOAuthSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
