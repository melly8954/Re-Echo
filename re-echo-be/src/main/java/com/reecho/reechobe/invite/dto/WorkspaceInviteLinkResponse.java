package com.reecho.reechobe.invite.dto;

import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import java.time.LocalDateTime;

// 클라이언트가 초대 링크를 구성할 수 있는 토큰과 만료 시각을 반환한다.
public record WorkspaceInviteLinkResponse(
        String token,
        LocalDateTime expiresAt
) {

    public static WorkspaceInviteLinkResponse from(WorkspaceInviteLink inviteLink) {
        return new WorkspaceInviteLinkResponse(
                inviteLink.getToken(),
                inviteLink.getExpiresAt()
        );
    }
}
