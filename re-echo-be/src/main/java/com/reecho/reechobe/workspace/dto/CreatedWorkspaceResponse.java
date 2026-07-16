package com.reecho.reechobe.workspace.dto;

import java.util.UUID;

// 워크스페이스 생성 직후 진입에 필요한 식별자를 반환한다.
public record CreatedWorkspaceResponse(
        UUID id,
        UUID defaultChannelId
) {
}
