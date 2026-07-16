package com.reecho.reechobe.channel.repository;

import java.util.UUID;

// 워크스페이스별 안읽음 채널 수를 목록 응답에 전달한다.
public interface WorkspaceUnreadChannelCountProjection {

    UUID getWorkspaceMembershipId();

    long getUnreadChannelCount();
}
