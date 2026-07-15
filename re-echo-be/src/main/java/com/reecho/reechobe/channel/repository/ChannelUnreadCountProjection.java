package com.reecho.reechobe.channel.repository;

import java.util.UUID;

// 채널 목록에 표시할 읽지 않은 활성 메시지 수를 전달한다.
public interface ChannelUnreadCountProjection {

    UUID getChannelId();

    long getUnreadCount();
}
