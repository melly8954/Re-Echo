package com.reecho.reechobe.realtime.service;

import com.reecho.reechobe.message.dto.ChannelMessageResponse;
import com.reecho.reechobe.realtime.dto.MessageEventPayload;
import com.reecho.reechobe.realtime.dto.RealtimeEventResponse;
import com.reecho.reechobe.realtime.dto.RealtimeEventType;
import com.reecho.reechobe.realtime.dto.TypingEventPayload;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 채널 구독자에게 문서화된 event envelope로 메시지와 typing 상태를 전파한다.
@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(UUID workspaceId, RealtimeEventType type, ChannelMessageResponse message) {
        messagingTemplate.convertAndSend(
                destination(workspaceId, message.channelId()),
                RealtimeEventResponse.of(type, new MessageEventPayload(message))
        );
    }

    public void publishTyping(UUID workspaceId, UUID channelId, List<UUID> typingUserIds) {
        messagingTemplate.convertAndSend(
                destination(workspaceId, channelId),
                RealtimeEventResponse.of(
                        RealtimeEventType.TYPING_UPDATED,
                        new TypingEventPayload(typingUserIds)
                )
        );
    }

    private String destination(UUID workspaceId, UUID channelId) {
        return "/sub/workspaces/" + workspaceId + "/channels/" + channelId;
    }
}
