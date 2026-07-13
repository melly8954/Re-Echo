package com.reecho.reechobe.channel.service.command;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.dto.CreateChannelRequest;
import com.reecho.reechobe.channel.dto.CreatedChannelResponse;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.CommonErrorCode;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 채널 생성과 초기 참여자 구성을 하나의 트랜잭션으로 처리한다.
@Service
@RequiredArgsConstructor
public class ChannelCommandService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;

    @Transactional
    public CreatedChannelResponse createChannel(UUID userId, UUID workspaceId, CreateChannelRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }

        WorkspaceMembership creatorMembership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        if (creatorMembership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED);
        }

        Channel channel = Channel.create(
                workspaceId,
                request.name(),
                request.description(),
                request.visibility(),
                creatorMembership.getId()
        );
        if (channelRepository.existsByWorkspaceIdAndName(workspaceId, channel.getName())) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "이미 사용 중인 채널 이름입니다.");
        }

        Set<UUID> initialMemberIds = initialMemberIds(request.memberIds(), creatorMembership.getId());
        validateInitialMembers(workspaceId, initialMemberIds);
        channelRepository.save(channel);
        initialMemberIds.forEach(memberId ->
                channelMembershipRepository.save(ChannelMembership.join(channel.getId(), memberId))
        );

        return new CreatedChannelResponse(channel.getId());
    }

    private Set<UUID> initialMemberIds(Set<UUID> requestedMemberIds, UUID creatorMembershipId) {
        Set<UUID> memberIds = new LinkedHashSet<>();
        memberIds.add(creatorMembershipId);
        if (requestedMemberIds != null) {
            memberIds.addAll(requestedMemberIds);
        }
        return memberIds;
    }

    private void validateInitialMembers(UUID workspaceId, Set<UUID> memberIds) {
        List<WorkspaceMembership> memberships = workspaceMembershipRepository.findAllById(memberIds);
        long validMembershipCount = memberships.stream()
                .filter(membership -> membership.getWorkspaceId().equals(workspaceId))
                .filter(membership -> membership.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .count();
        if (validMembershipCount != memberIds.size()) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
