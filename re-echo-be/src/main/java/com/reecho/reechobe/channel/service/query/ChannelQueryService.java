package com.reecho.reechobe.channel.service.query;

import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.dto.ChannelListItemResponse;
import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 워크스페이스 멤버 기준으로 접근 가능한 채널 목록을 구성한다.
@Service
@RequiredArgsConstructor
public class ChannelQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;

    @Transactional(readOnly = true)
    public ChannelListResponse getChannels(UUID userId, UUID workspaceId) {
        workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));

        Set<UUID> joinedChannelIds = Set.copyOf(
                channelMembershipRepository.findChannelIdsByWorkspaceMembershipIdAndStatus(
                        membership.getId(),
                        ChannelMembershipStatus.ACTIVE
                )
        );
        return ChannelListResponse.of(
                channelRepository.findAccessibleActiveChannels(workspaceId, membership.getId())
                        .stream()
                        .map(channel -> ChannelListItemResponse.from(
                                channel,
                                joinedChannelIds.contains(channel.getId())
                        ))
                        .toList()
        );
    }
}
