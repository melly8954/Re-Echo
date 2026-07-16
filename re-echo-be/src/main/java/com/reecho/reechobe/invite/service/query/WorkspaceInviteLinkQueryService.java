package com.reecho.reechobe.invite.service.query;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLinkStatus;
import com.reecho.reechobe.invite.dto.WorkspaceInviteLinkResponse;
import com.reecho.reechobe.invite.dto.WorkspaceInvitePreviewResponse;
import com.reecho.reechobe.invite.exception.InviteErrorCode;
import com.reecho.reechobe.invite.repository.WorkspaceInviteLinkRepository;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 초대 링크 조회와 공개 미리보기 정보를 구성한다.
@Service
@RequiredArgsConstructor
public class WorkspaceInviteLinkQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceInviteLinkRepository workspaceInviteLinkRepository;

    @Transactional
    // 초대 권한이 있는 멤버에게만 현재 활성 초대 링크를 반환한다.
    public WorkspaceInviteLinkResponse getActiveInviteLink(UUID userId, UUID workspaceId) {
        validateInviteIssuer(userId, workspaceId);
        WorkspaceInviteLink inviteLink = workspaceInviteLinkRepository
                .findByWorkspaceIdAndStatus(workspaceId, WorkspaceInviteLinkStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(InviteErrorCode.INVITE_NOT_FOUND));
        validateUsable(inviteLink);

        return WorkspaceInviteLinkResponse.from(inviteLink);
    }

    @Transactional
    // 로그인 전에도 참여 대상을 확인할 수 있도록 유효한 초대의 최소 정보만 반환한다.
    public WorkspaceInvitePreviewResponse previewInviteLink(String token) {
        WorkspaceInviteLink inviteLink = workspaceInviteLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(InviteErrorCode.INVITE_NOT_FOUND));
        validateUsable(inviteLink);
        Workspace workspace = workspaceRepository.findById(inviteLink.getWorkspaceId())
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        return WorkspaceInvitePreviewResponse.of(workspace, inviteLink);
    }

    // 링크 조회는 워크스페이스 소유자 또는 관리자 권한으로 제한한다.
    private void validateInviteIssuer(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }

        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        if (membership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }
    }

    // 폐기되었거나 만료된 토큰은 공개 미리보기와 참여 모두에서 거부한다.
    private void validateUsable(WorkspaceInviteLink inviteLink) {
        if (inviteLink.getStatus() == WorkspaceInviteLinkStatus.REVOKED) {
            throw new BusinessException(InviteErrorCode.INVITE_REVOKED);
        }
        if (inviteLink.getStatus() == WorkspaceInviteLinkStatus.EXPIRED
                || inviteLink.isExpired(LocalDateTime.now())) {
            inviteLink.expire();
            throw new BusinessException(InviteErrorCode.INVITE_EXPIRED);
        }
    }
}
