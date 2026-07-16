package com.reecho.reechobe.channel.service.query;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import com.reecho.reechobe.channel.dto.ChannelListItemResponse;
import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.dto.WorkspaceMemberListResponse;
import com.reecho.reechobe.member.dto.WorkspaceMemberResponse;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    // 현재 워크스페이스 멤버가 접근 가능한 채널과 참여·안 읽음 상태를 함께 구성한다.
    public ChannelListResponse getChannels(UUID userId, UUID workspaceId) {
        validateExistingWorkspace(workspaceId);
        WorkspaceMembership membership = getActiveWorkspaceMembership(userId, workspaceId);

        Set<UUID> joinedChannelIds = Set.copyOf(
                channelMembershipRepository.findChannelIdsByWorkspaceMembershipIdAndStatus(
                        membership.getId(),
                        ChannelMembershipStatus.ACTIVE
                )
        );
        List<Channel> channels = channelRepository.findAccessibleActiveChannels(
                workspaceId,
                membership.getId()
        );
        Map<UUID, Long> memberCountByChannelId = channelMembershipRepository
                .countByChannelIdsAndStatus(
                        channels.stream().map(Channel::getId).toList(),
                        ChannelMembershipStatus.ACTIVE
                )
                .stream()
                .collect(Collectors.toMap(
                        channelMemberCount -> channelMemberCount.getChannelId(),
                        channelMemberCount -> channelMemberCount.getMemberCount()
                ));
        List<UUID> channelIds = channels.stream().map(Channel::getId).toList();
        Map<UUID, Long> unreadCountByChannelId = channelIds.isEmpty()
                ? Map.of()
                : channelMembershipRepository.countUnreadActiveMessagesByChannelIds(membership.getId(), channelIds)
                        .stream()
                        .collect(Collectors.toMap(
                                unreadCount -> unreadCount.getChannelId(),
                                unreadCount -> unreadCount.getUnreadCount()
                        ));

        return ChannelListResponse.of(
                channels.stream()
                        .map(channel -> ChannelListItemResponse.from(
                                channel,
                                joinedChannelIds.contains(channel.getId()),
                                membership.getId(),
                                memberCountByChannelId.getOrDefault(channel.getId(), 0L),
                                unreadCountByChannelId.getOrDefault(channel.getId(), 0L)
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    // 공개 범위 또는 활성 참여 여부를 확인한 뒤 채널 멤버 목록을 권한순으로 반환한다.
    public WorkspaceMemberListResponse getChannelMembers(UUID userId, UUID workspaceId, UUID channelId) {
        validateExistingWorkspace(workspaceId);
        WorkspaceMembership membership = getActiveWorkspaceMembership(userId, workspaceId);
        Channel channel = channelRepository.findById(channelId)
                .filter(foundChannel -> foundChannel.getWorkspaceId().equals(workspaceId))
                .filter(foundChannel -> foundChannel.getStatus() == ChannelStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND));
        if (channel.getVisibility() == ChannelVisibility.PRIVATE) {
            channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channelId, membership.getId())
                    .filter(channelMembership -> channelMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
        }

        List<WorkspaceMemberResponse> members = workspaceMembershipRepository.findActiveChannelMembers(
                        channelId,
                        WorkspaceMembershipStatus.ACTIVE,
                        ChannelMembershipStatus.ACTIVE
                )
                .stream()
                .sorted(Comparator
                        .comparingInt((WorkspaceMembership channelMember) -> roleOrder(channelMember.getRole()))
                        .thenComparing(WorkspaceMembership::getDisplayName))
                .map(WorkspaceMemberResponse::from)
                .toList();
        return WorkspaceMemberListResponse.of(members);
    }

    // 삭제된 워크스페이스는 멤버십이 남아 있어도 조회 대상으로 허용하지 않는다.
    private void validateExistingWorkspace(UUID workspaceId) {
        workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    // 비활성 또는 탈퇴 멤버가 채널 정보로 우회 접근하지 못하도록 활성 멤버십만 찾는다.
    private WorkspaceMembership getActiveWorkspaceMembership(UUID userId, UUID workspaceId) {
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    // 멤버 목록에서 권한이 높은 사용자를 먼저 보여주기 위한 정렬 우선순위다.
    private int roleOrder(WorkspaceMembershipRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }
}
