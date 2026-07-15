package com.reecho.reechobe.realtime.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// WebSocket 연결을 허용할 프런트엔드 origin과 입력 중 상태 TTL을 설정한다.
@ConfigurationProperties(prefix = "reecho.websocket")
public record WebSocketProperties(
        List<String> allowedOrigins,
        long typingTtlSeconds
) {

    public WebSocketProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        if (typingTtlSeconds <= 0) {
            typingTtlSeconds = 5;
        }
    }
}
