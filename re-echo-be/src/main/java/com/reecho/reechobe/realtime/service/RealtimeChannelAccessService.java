package com.reecho.reechobe.realtime.service;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// STOMP 구독과 발행에 REST API와 같은 워크스페이스·채널 접근 규칙을 적용한다.
@Service
@RequiredArgsConstructor
public class RealtimeChannelAccessService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;

    @Transactional(readOnly = true)
    public void validateReadable(UUID userId, UUID workspaceId, UUID channelId) {
        WorkspaceMembership membership = getActiveMembership(userId, workspaceId);
        Channel channel = getChannel(workspaceId, channelId);
        if (channel.getStatus() == ChannelStatus.DELETED) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }
        requireActiveChannelMembership(channelId, membership.getId());
    }

    @Transactional(readOnly = true)
    public void validateWritable(UUID userId, UUID workspaceId, UUID channelId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
        WorkspaceMembership membership = getActiveMembership(userId, workspaceId);
        Channel channel = getChannel(workspaceId, channelId);
        if (channel.getStatus() == ChannelStatus.ARCHIVED) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ARCHIVED);
        }
        if (channel.getStatus() == ChannelStatus.DELETED) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND);
        }
        requireActiveChannelMembership(channelId, membership.getId());
    }

    private WorkspaceMembership getActiveMembership(UUID userId, UUID workspaceId) {
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(workspaceId, userId, WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    private Channel getChannel(UUID workspaceId, UUID channelId) {
        return channelRepository.findById(channelId)
                .filter(channel -> channel.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND));
    }

    private void requireActiveChannelMembership(UUID channelId, UUID membershipId) {
        channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channelId, membershipId)
                .filter(channelMembership -> channelMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
    }
}
