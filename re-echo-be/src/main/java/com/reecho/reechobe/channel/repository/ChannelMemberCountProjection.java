package com.reecho.reechobe.channel.repository;

import java.util.UUID;

// 채널 목록에 표시할 활성 멤버 수 집계 결과를 전달한다.
public interface ChannelMemberCountProjection {

    UUID getChannelId();

    long getMemberCount();
}
