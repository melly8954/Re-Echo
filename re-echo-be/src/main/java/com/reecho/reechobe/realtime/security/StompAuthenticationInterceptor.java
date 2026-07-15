package com.reecho.reechobe.realtime.security;

import com.reecho.reechobe.auth.jwt.JwtVerifyService;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.realtime.service.RealtimeChannelAccessService;
import java.util.UUID;
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
    private final RealtimeChannelAccessService realtimeChannelAccessService;

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
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            validateChannelDestination(accessor);
        }
        return org.springframework.messaging.support.MessageBuilder.createMessage(
                message.getPayload(),
                accessor.getMessageHeaders()
        );
    }

    private void validateChannelDestination(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof StompUserPrincipal principal)) {
            throw new IllegalArgumentException("WebSocket 인증 정보가 필요합니다.");
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new IllegalArgumentException("WebSocket 대상 경로가 필요합니다.");
        }
        String[] segments = destination.split("/");
        boolean subscription = StompCommand.SUBSCRIBE.equals(accessor.getCommand());
        boolean validSubscription = subscription
                && segments.length == 6
                && "sub".equals(segments[1])
                && "workspaces".equals(segments[2])
                && "channels".equals(segments[4]);
        boolean validPublication = !subscription
                && segments.length == 7
                && "pub".equals(segments[1])
                && "workspaces".equals(segments[2])
                && "channels".equals(segments[4])
                && ("messages".equals(segments[6]) || "typing".equals(segments[6]));
        if (!validSubscription && !validPublication) {
            throw new IllegalArgumentException("WebSocket 대상 경로가 올바르지 않습니다.");
        }
        UUID workspaceId = UUID.fromString(segments[3]);
        UUID channelId = UUID.fromString(segments[5]);
        if (subscription) {
            realtimeChannelAccessService.validateReadable(principal.userId(), workspaceId, channelId);
            return;
        }
        realtimeChannelAccessService.validateWritable(principal.userId(), workspaceId, channelId);
    }
}
