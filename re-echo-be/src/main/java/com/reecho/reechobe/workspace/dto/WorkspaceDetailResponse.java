package com.reecho.reechobe.workspace.dto;

import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import java.util.UUID;

// 워크스페이스 진입에 필요한 기본 정보와 내 멤버십을 반환한다.
public record WorkspaceDetailResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        UUID imageFileId,
        WorkspaceStatus status,
        MyWorkspaceMembershipResponse myMembership,
        UUID defaultChannelId,
        boolean canRestore
) {

    public static WorkspaceDetailResponse of(
            Workspace workspace,
            WorkspaceMembership membership,
            UUID defaultChannelId,
            boolean canRestore
    ) {
        return new WorkspaceDetailResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getImageUrl(),
                workspace.getImageFileId(),
                workspace.getStatus(),
                MyWorkspaceMembershipResponse.from(membership),
                defaultChannelId,
                canRestore
        );
    }

    // 현재 사용자의 워크스페이스 멤버십 정보를 표현한다.
    public record MyWorkspaceMembershipResponse(
            UUID id,
            String displayName,
            String profileImageUrl,
            WorkspaceMembershipRole role,
            WorkspaceMembershipStatus status
    ) {

        public static MyWorkspaceMembershipResponse from(WorkspaceMembership membership) {
            return new MyWorkspaceMembershipResponse(
                    membership.getId(),
                    membership.getDisplayName(),
                    membership.getProfileImageUrl(),
                    membership.getRole(),
                    membership.getStatus()
            );
        }
    }
}
