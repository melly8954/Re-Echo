package com.reecho.reechobe.workspace.service.query;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.dto.WorkspaceDetailResponse;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 워크스페이스 진입에 필요한 조회 모델을 구성한다.
@Service
@RequiredArgsConstructor
public class WorkspaceQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;

    @Transactional(readOnly = true)
    public WorkspaceDetailResponse getWorkspaceDetail(UUID userId, UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        Channel defaultChannel = channelRepository.findByWorkspaceIdAndGeneralTrue(workspaceId)
                .orElseThrow(() -> new IllegalStateException("기본 채널이 없습니다."));

        return WorkspaceDetailResponse.of(
                workspace,
                membership,
                defaultChannel.getId(),
                canRestore(workspace, membership)
        );
    }

    private boolean canRestore(Workspace workspace, WorkspaceMembership membership) {
        return workspace.getStatus() == WorkspaceStatus.ARCHIVED
                && membership.getRole() == WorkspaceMembershipRole.OWNER
                && workspace.getArchiveExpiresAt() != null
                && LocalDateTime.now().isBefore(workspace.getArchiveExpiresAt());
    }
}
