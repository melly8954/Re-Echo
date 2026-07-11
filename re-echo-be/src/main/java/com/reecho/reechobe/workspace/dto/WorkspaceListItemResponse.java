package com.reecho.reechobe.workspace.dto;

import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import java.time.LocalDateTime;
import java.util.UUID;

// 워크스페이스 시작 화면의 목록 항목 정보를 전달한다.
public record WorkspaceListItemResponse(
        UUID id,
        String name,
        String imageUrl,
        WorkspaceMembershipRole role,
        WorkspaceStatus status,
        LocalDateTime lastVisitedAt,
        UUID defaultChannelId,
        int unreadChannelCount
) {

    public static WorkspaceListItemResponse of(
            Workspace workspace,
            WorkspaceMembership membership,
            UUID defaultChannelId
    ) {
        return new WorkspaceListItemResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getImageUrl(),
                membership.getRole(),
                workspace.getStatus(),
                membership.getLastVisitedAt(),
                defaultChannelId,
                0
        );
    }
}
