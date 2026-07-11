package com.reecho.reechobe.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtVerifyService jwtVerifyService;

    @Mock
    private ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtVerifyService, authenticationEntryPoint);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_access_token이면_인증_사용자를_security_context에_저장한다() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-access-token");
        when(jwtVerifyService.verifyAccessToken("valid-access-token")).thenReturn(userId);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedUserPrincipal(userId));
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(authenticationEntryPoint);
    }

    @Test
    void authorization_header가_없으면_인증_없이_다음_필터로_진행한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtVerifyService, authenticationEntryPoint);
    }

    @Test
    void bearer_형식이_아니면_인증_실패_응답을_작성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");

        filter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(jwtVerifyService);
    }

    @Test
    void 유효하지_않은_access_token이면_인증_실패_응답을_작성한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-access-token");
        when(jwtVerifyService.verifyAccessToken("invalid-access-token"))
                .thenThrow(new BusinessException(AuthErrorCode.AUTH_UNAUTHORIZED));

        filter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint).commence(
                any(MockHttpServletRequest.class),
                any(MockHttpServletResponse.class),
                any(AuthenticationException.class)
        );
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
