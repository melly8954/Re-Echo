package com.reecho.reechobe.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reecho.reechobe.auth.exception.AuthErrorCode;
import com.reecho.reechobe.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

// 인증 실패를 REST API 공통 응답 형식으로 변환한다.
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    // 인증되지 않은 요청에 AUTH_UNAUTHORIZED 응답을 작성한다.
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        AuthErrorCode errorCode = AuthErrorCode.AUTH_UNAUTHORIZED;
        ApiResponse<Void> body = ApiResponse.error(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                null
        );

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
