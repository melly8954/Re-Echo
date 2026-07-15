package com.reecho.reechobe.realtime.security;

import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

// STOMP CONNECT frame의 Bearer Access Token을 검증해 세션 사용자로 등록한다.
@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtVerifyService jwtVerifyService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                throw new IllegalArgumentException("WebSocket 인증 정보가 필요합니다.");
            }
            try {
                accessor.setUser(new StompUserPrincipal(
                        jwtVerifyService.verifyAccessToken(authorization.substring(BEARER_PREFIX.length()).trim())
                ));
            } catch (BusinessException exception) {
                throw new IllegalArgumentException("WebSocket 인증에 실패했습니다.", exception);
            }
        }
        return message;
    }
}
