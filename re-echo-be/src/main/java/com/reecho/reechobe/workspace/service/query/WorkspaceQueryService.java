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
import com.reecho.reechobe.workspace.dto.WorkspaceListItemResponse;
import com.reecho.reechobe.workspace.dto.WorkspaceListResponse;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    @Transactional
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
        membership.visit();

        return WorkspaceDetailResponse.of(
                workspace,
                membership,
                defaultChannel.getId(),
                canRestore(workspace, membership)
        );
    }

    @Transactional(readOnly = true)
    public WorkspaceListResponse getWorkspaceList(UUID userId) {
        List<WorkspaceMembership> memberships = workspaceMembershipRepository
                .findByUserIdAndStatusOrderByJoinedAtAscIdAsc(userId, WorkspaceMembershipStatus.ACTIVE);
        if (memberships.isEmpty()) {
            return WorkspaceListResponse.of(List.of());
        }

        List<UUID> workspaceIds = memberships.stream()
                .map(WorkspaceMembership::getWorkspaceId)
                .toList();
        Map<UUID, Workspace> workspacesById = workspaceRepository.findAllById(workspaceIds)
                .stream()
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .collect(Collectors.toMap(Workspace::getId, Function.identity()));
        Map<UUID, Channel> generalChannelsByWorkspaceId = channelRepository
                .findByWorkspaceIdInAndGeneralTrue(workspaceIds)
                .stream()
                .collect(Collectors.toMap(Channel::getWorkspaceId, Function.identity()));

        return WorkspaceListResponse.of(
                memberships.stream()
                        .filter(membership -> workspacesById.containsKey(membership.getWorkspaceId()))
                        .map(membership -> {
                            Workspace workspace = workspacesById.get(membership.getWorkspaceId());
                            Channel generalChannel = generalChannelsByWorkspaceId.get(workspace.getId());
                            if (generalChannel == null) {
                                throw new IllegalStateException("기본 채널이 없습니다.");
                            }
                            return WorkspaceListItemResponse.of(workspace, membership, generalChannel.getId());
                        })
                        .toList()
        );
    }

    private boolean canRestore(Workspace workspace, WorkspaceMembership membership) {
        return workspace.getStatus() == WorkspaceStatus.ARCHIVED
                && membership.getRole() == WorkspaceMembershipRole.OWNER
                && workspace.getArchiveExpiresAt() != null
                && LocalDateTime.now().isBefore(workspace.getArchiveExpiresAt());
    }
}
