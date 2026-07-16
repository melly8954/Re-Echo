package com.reecho.reechobe.workspace.service.command;

import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
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

// 워크스페이스와 하위 채널의 보관 및 복원 상태 전환을 처리한다.
@Service
@RequiredArgsConstructor
public class WorkspaceLifecycleCommandService {

    private static final int ARCHIVE_RETENTION_DAYS = 15;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;

    // 소유자 요청으로 활성 워크스페이스와 활성 채널을 같은 보관 시각으로 전환한다.
    @Transactional
    // 소유자 요청으로 워크스페이스와 하위 채널을 복원 가능 기간의 보관 상태로 전환한다.
    public void archiveWorkspace(UUID userId, UUID workspaceId) {
        Workspace workspace = requireWorkspace(workspaceId);
        requireOwner(userId, workspaceId);
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }

        LocalDateTime archivedAt = LocalDateTime.now();
        LocalDateTime archiveExpiresAt = archivedAt.plusDays(ARCHIVE_RETENTION_DAYS);
        workspace.archive(archivedAt, archiveExpiresAt);
        channelRepository.findByWorkspaceIdAndStatus(workspaceId, ChannelStatus.ACTIVE)
                .forEach(channel -> channel.archive(archivedAt, archiveExpiresAt));
    }

    // 만료 전 소유자 요청으로 워크스페이스와 이 보관 작업에 포함된 채널만 복원한다.
    @Transactional
    // 소유자가 만료 전 보관 워크스페이스와 하위 채널을 다시 활성화한다.
    public void restoreWorkspace(UUID userId, UUID workspaceId) {
        Workspace workspace = requireWorkspace(workspaceId);
        requireOwner(userId, workspaceId);
        if (!canRestore(workspace)) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_RESTORE_NOT_ALLOWED);
        }

        LocalDateTime archivedAt = workspace.getArchivedAt();
        workspace.restore();
        channelRepository.findByWorkspaceIdAndStatusAndArchivedAt(
                        workspaceId,
                        ChannelStatus.ARCHIVED,
                        archivedAt
                )
                .forEach(channel -> channel.restore());
    }

    private Workspace requireWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    // 생명주기 변경은 관리자보다 강한 소유자 권한으로 제한한다.
    private void requireOwner(UUID userId, UUID workspaceId) {
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        if (membership.getRole() != WorkspaceMembershipRole.OWNER) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }
    }

    // 보관 만료 시각을 넘긴 워크스페이스는 복원할 수 없게 한다.
    private boolean canRestore(Workspace workspace) {
        return workspace.getStatus() == WorkspaceStatus.ARCHIVED
                && workspace.getArchivedAt() != null
                && workspace.getArchiveExpiresAt() != null
                && LocalDateTime.now().isBefore(workspace.getArchiveExpiresAt());
    }
}
