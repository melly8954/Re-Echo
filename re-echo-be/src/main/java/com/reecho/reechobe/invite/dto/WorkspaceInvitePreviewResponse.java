package com.reecho.reechobe.invite.dto;

import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import com.reecho.reechobe.workspace.domain.Workspace;
import java.time.LocalDateTime;

// 참여 전 확인에 필요한 최소 워크스페이스 정보를 반환한다.
public record WorkspaceInvitePreviewResponse(
        String workspaceName,
        String workspaceImageUrl,
        LocalDateTime expiresAt
) {

    public static WorkspaceInvitePreviewResponse of(
            Workspace workspace,
            WorkspaceInviteLink inviteLink
    ) {
        return new WorkspaceInvitePreviewResponse(
                workspace.getName(),
                workspace.getImageUrl(),
                inviteLink.getExpiresAt()
        );
    }
}
