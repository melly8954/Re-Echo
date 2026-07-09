package com.reecho.reechobe.auth.handler;

import com.reecho.reechobe.auth.config.OAuth2LoginProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

// OAuth 로그인 실패 시 프론트엔드 로그인 화면으로 이동시킨다.
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuth2LoginProperties properties;

    // 실패 원인을 노출하지 않고 공통 실패 URL로 리다이렉트한다.
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        if (properties.getFailureRedirectUri() == null || properties.getFailureRedirectUri().isBlank()) {
            throw new IllegalStateException("OAuth 로그인 실패 리다이렉트 URI 설정은 필수입니다.");
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(properties.getFailureRedirectUri());
    }
}
