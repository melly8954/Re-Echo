package com.reecho.reechobe.invite.dto;

import java.util.UUID;

// 초대 참여 직후 워크스페이스 진입에 필요한 식별자를 반환한다.
public record JoinedWorkspaceResponse(
        UUID id,
        UUID defaultChannelId
) {
}
