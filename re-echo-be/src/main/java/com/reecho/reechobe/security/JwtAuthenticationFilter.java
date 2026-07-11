package com.reecho.reechobe.security;

import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Bearer Access Token을 검증해 요청 단위 인증 정보를 구성한다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerifyService jwtVerifyService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    // 유효한 Access Token의 사용자 식별자를 Security Context에 저장한다.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            commenceUnauthorized(request, response);
            return;
        }

        String accessToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (accessToken.isBlank()) {
            commenceUnauthorized(request, response);
            return;
        }

        UUID userId;
        try {
            userId = jwtVerifyService.verifyAccessToken(accessToken);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            commenceUnauthorized(request, response);
            return;
        }

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private void commenceUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Access Token 인증에 실패했습니다.")
        );
    }
}
