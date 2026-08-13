package com.reecho.reechobe.realtime.controller;

import com.reecho.reechobe.message.dto.CreateMessageRequest;
import com.reecho.reechobe.message.service.command.MessageCommandService;
import com.reecho.reechobe.realtime.dto.TypingPublishRequest;
import com.reecho.reechobe.realtime.security.StompUserPrincipal;
import com.reecho.reechobe.realtime.service.TypingStateService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

// STOMP 발행을 기존 메시지 명령과 입력 중 상태 갱신으로 연결한다.
@Controller
@RequiredArgsConstructor
public class RealtimeMessageController {

    private final MessageCommandService messageCommandService;
    private final TypingStateService typingStateService;

    // 채널 멤버의 STOMP 메시지 발행을 영속화하고 구독자 event로 전파한다.
    @MessageMapping("workspaces/{workspaceId}/channels/{channelId}/messages")
    public void createMessage(
            StompUserPrincipal principal,
            @DestinationVariable UUID workspaceId,
            @DestinationVariable UUID channelId,
            @Payload CreateMessageRequest request
    ) {
        messageCommandService.createMessage(principal.userId(), workspaceId, channelId, request);
    }

    // 채널 멤버의 입력 중 상태를 Redis TTL snapshot으로 갱신한다.
    @MessageMapping("workspaces/{workspaceId}/channels/{channelId}/typing")
    public void updateTyping(
            StompUserPrincipal principal,
            @DestinationVariable UUID workspaceId,
            @DestinationVariable UUID channelId,
            @Payload TypingPublishRequest request
    ) {
        typingStateService.update(workspaceId, channelId, principal.userId(), request.typing());
    }
}
