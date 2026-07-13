package com.reecho.reechobe.member.service.command;

import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 워크스페이스 멤버십의 자진 탈퇴 상태 변경을 처리한다.
@Service
@RequiredArgsConstructor
public class WorkspaceMemberCommandService {

    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelMembershipRepository channelMembershipRepository;

    @Transactional
    public void leaveWorkspace(UUID userId, UUID workspaceId, UUID memberId) {
        WorkspaceMembership membership = getActiveMembership(userId, workspaceId);
        if (!membership.getId().equals(memberId)) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        if (membership.getRole() == WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(MemberErrorCode.MEMBER_LAST_OWNER_LEAVE_FORBIDDEN);
        }

        membership.leave();
        leaveActiveChannelMemberships(membership.getId());
    }

    private WorkspaceMembership getActiveMembership(UUID userId, UUID workspaceId) {
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    // 재참여 시 기본 채널만 자동 복구되도록 기존 채널 참여도 함께 종료한다.
    private void leaveActiveChannelMemberships(UUID workspaceMembershipId) {
        channelMembershipRepository
                .findByWorkspaceMembershipIdAndStatus(
                        workspaceMembershipId,
                        ChannelMembershipStatus.ACTIVE
                )
                .forEach(ChannelMembership::leave);
    }
}
