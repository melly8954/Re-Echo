package com.reecho.reechobe.channel.service.command;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelReadState;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import com.reecho.reechobe.channel.dto.AddChannelMembersRequest;
import com.reecho.reechobe.channel.dto.CreateChannelRequest;
import com.reecho.reechobe.channel.dto.CreatedChannelResponse;
import com.reecho.reechobe.channel.dto.UpdateChannelRequest;
import com.reecho.reechobe.channel.dto.UpdateChannelReadStateRequest;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelReadStateRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.exception.CommonErrorCode;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.exception.MemberErrorCode;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.message.domain.Message;
import com.reecho.reechobe.message.exception.MessageErrorCode;
import com.reecho.reechobe.message.repository.MessageRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 채널 생성과 채널 참여 상태 변경을 트랜잭션으로 처리한다.
@Service
@RequiredArgsConstructor
public class ChannelCommandService {

    private static final int ARCHIVE_RETENTION_DAYS = 15;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;
    private final ChannelReadStateRepository channelReadStateRepository;
    private final MessageRepository messageRepository;

    @Transactional
    // 생성자의 워크스페이스 권한을 확인한 뒤 채널과 초기 참여 상태를 함께 만든다.
    public CreatedChannelResponse createChannel(UUID userId, UUID workspaceId, CreateChannelRequest request) {
        validateActiveWorkspace(workspaceId);
        WorkspaceMembership creatorMembership = getActiveWorkspaceMembership(userId, workspaceId);
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

    @Transactional
    // 관리자 요청으로 활성 채널의 이름과 설명을 함께 변경한다.
    public void updateChannel(UUID userId, UUID workspaceId, UUID channelId, UpdateChannelRequest request) {
        validateActiveWorkspace(workspaceId);
        requireChannelManager(userId, workspaceId);
        Channel channel = getActiveWorkspaceChannel(workspaceId, channelId);
        String requestedName = request.name().trim();
        if (!channel.getName().equals(requestedName)
                && channelRepository.existsByWorkspaceIdAndName(workspaceId, requestedName)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "이미 사용 중인 채널 이름입니다.");
        }
        channel.update(request.name(), request.description());
    }

    @Transactional
    // 관리자가 채널을 15일간 읽기 전용으로 전환한다.
    public void archiveChannel(UUID userId, UUID workspaceId, UUID channelId) {
        validateActiveWorkspace(workspaceId);
        requireChannelManager(userId, workspaceId);
        Channel channel = getReadableWorkspaceChannel(workspaceId, channelId);
        if (channel.getStatus() == ChannelStatus.ARCHIVED) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ARCHIVED);
        }
        LocalDateTime archivedAt = LocalDateTime.now();
        channel.archive(archivedAt, archivedAt.plusDays(ARCHIVE_RETENTION_DAYS));
    }

    @Transactional
    // 관리자가 삭제 예정 시각 전의 보관 채널을 다시 활성화한다.
    public void restoreChannel(UUID userId, UUID workspaceId, UUID channelId) {
        validateActiveWorkspace(workspaceId);
        requireChannelManager(userId, workspaceId);
        Channel channel = getReadableWorkspaceChannel(workspaceId, channelId);
        if (!canRestore(channel)) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_RESTORE_NOT_ALLOWED);
        }
        channel.restore();
    }

    @Transactional
    // 공개 채널에 이미 참여한 경우도 같은 활성 참여 상태로 수렴시킨다.
    public void joinPublicChannel(UUID userId, UUID workspaceId, UUID channelId) {
        validateActiveWorkspace(workspaceId);
        WorkspaceMembership membership = getActiveWorkspaceMembership(userId, workspaceId);
        Channel channel = getActiveWorkspaceChannel(workspaceId, channelId);
        if (channel.getVisibility() != ChannelVisibility.PUBLIC) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_JOIN_FORBIDDEN);
        }

        channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channelId, membership.getId())
                .ifPresentOrElse(channelMembership -> {
                    if (channelMembership.getStatus() == ChannelMembershipStatus.ACTIVE) {
                        throw new BusinessException(ChannelErrorCode.CHANNEL_ALREADY_JOINED);
                    }
                    if (channelMembership.getStatus() == ChannelMembershipStatus.REMOVED) {
                        throw new BusinessException(ChannelErrorCode.CHANNEL_MEMBER_REMOVED);
                    }
                    channelMembership.rejoin();
                }, () -> channelMembershipRepository.save(
                        ChannelMembership.join(channelId, membership.getId())
                ));
    }

    @Transactional
    // 비공개 채널 생성자만 워크스페이스 멤버를 추가하거나 재참여시킨다.
    public void addPrivateChannelMembers(
            UUID userId,
            UUID workspaceId,
            UUID channelId,
            AddChannelMembersRequest request
    ) {
        validateActiveWorkspace(workspaceId);
        WorkspaceMembership requesterMembership = getActiveWorkspaceMembership(userId, workspaceId);
        if (requesterMembership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED);
        }

        Channel channel = getActiveWorkspaceChannel(workspaceId, channelId);
        if (channel.getVisibility() != ChannelVisibility.PRIVATE) {
            throw new BusinessException(
                    CommonErrorCode.VALIDATION_ERROR,
                    "비공개 채널에만 멤버를 추가할 수 있습니다."
            );
        }

        Set<UUID> memberIds = new LinkedHashSet<>(request.memberIds());
        validateInitialMembers(workspaceId, memberIds);
        memberIds.forEach(memberId -> addOrReactivatePrivateChannelMember(channelId, memberId));
    }

    @Transactional
    // 현재 사용자의 채널 참여 상태만 탈퇴로 전환한다.
    public void leaveChannel(UUID userId, UUID workspaceId, UUID channelId) {
        validateActiveWorkspace(workspaceId);
        WorkspaceMembership membership = getActiveWorkspaceMembership(userId, workspaceId);
        Channel channel = getActiveWorkspaceChannel(workspaceId, channelId);
        if (channel.isGeneral()) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_GENERAL_LEAVE_FORBIDDEN);
        }
        if (isPrivateChannelCreator(channel, membership.getId())) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_CREATOR_LEAVE_FORBIDDEN);
        }

        ChannelMembership channelMembership = channelMembershipRepository
                .findByChannelIdAndWorkspaceMembershipId(channelId, membership.getId())
                .filter(foundMembership -> foundMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
        channelMembership.leave();
    }

    @Transactional
    // 비공개 채널 생성자가 대상 멤버의 접근 권한을 제거한다.
    public void removeChannelMember(UUID userId, UUID workspaceId, UUID channelId, UUID memberId) {
        validateActiveWorkspace(workspaceId);
        WorkspaceMembership requesterMembership = getActiveWorkspaceMembership(userId, workspaceId);
        if (requesterMembership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED);
        }

        Channel channel = getActiveWorkspaceChannel(workspaceId, channelId);
        if (channel.isGeneral()) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_GENERAL_LEAVE_FORBIDDEN);
        }
        validateTargetWorkspaceMember(workspaceId, memberId);
        if (isPrivateChannelCreator(channel, memberId)) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_CREATOR_LEAVE_FORBIDDEN);
        }

        ChannelMembership channelMembership = channelMembershipRepository
                .findByChannelIdAndWorkspaceMembershipId(channelId, memberId)
                .filter(foundMembership -> foundMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
        channelMembership.remove();
    }

    @Transactional
    // 이전 읽음 위치로 되돌아가지 않도록 더 최신 메시지일 때만 상태를 갱신한다.
    public void updateChannelReadState(
            UUID userId,
            UUID workspaceId,
            UUID channelId,
            UpdateChannelReadStateRequest request
    ) {
        WorkspaceMembership membership = getReadableWorkspaceMembership(userId, workspaceId);
        Channel channel = getReadableWorkspaceChannel(workspaceId, channelId);
        ChannelMembership channelMembership = channelMembershipRepository
                .findByChannelIdAndWorkspaceMembershipId(channel.getId(), membership.getId())
                .filter(foundMembership -> foundMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
        Message requestedMessage = messageRepository.findById(request.lastReadMessageId())
                .filter(message -> message.getChannelId().equals(channelId))
                .orElseThrow(() -> new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND));

        channelReadStateRepository.findByChannelMembershipId(channelMembership.getId())
                .ifPresentOrElse(
                        readState -> advanceReadState(readState, requestedMessage),
                        () -> channelReadStateRepository.save(
                                ChannelReadState.create(channelMembership.getId(), requestedMessage.getId())
                        )
                );
    }

    private void validateActiveWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
    }

    private WorkspaceMembership getActiveWorkspaceMembership(UUID userId, UUID workspaceId) {
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    // 채널 설정과 수명주기 변경은 소유자 또는 관리자 역할로 제한한다.
    private WorkspaceMembership requireChannelManager(UUID userId, UUID workspaceId) {
        WorkspaceMembership membership = getActiveWorkspaceMembership(userId, workspaceId);
        if (membership.getRole() == WorkspaceMembershipRole.MEMBER) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED);
        }
        return membership;
    }

    private WorkspaceMembership getReadableWorkspaceMembership(UUID userId, UUID workspaceId) {
        workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        return workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(workspaceId, userId, WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
    }

    private Channel getActiveWorkspaceChannel(UUID workspaceId, UUID channelId) {
        return channelRepository.findById(channelId)
                .filter(channel -> channel.getWorkspaceId().equals(workspaceId))
                .filter(channel -> channel.getStatus() == ChannelStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND));
    }

    private Channel getReadableWorkspaceChannel(UUID workspaceId, UUID channelId) {
        return channelRepository.findById(channelId)
                .filter(channel -> channel.getWorkspaceId().equals(workspaceId))
                .filter(channel -> channel.getStatus() != ChannelStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND));
    }

    // 보관 만료 시각을 넘긴 채널은 자동 삭제 전후에 복원할 수 없게 한다.
    private boolean canRestore(Channel channel) {
        return channel.getStatus() == ChannelStatus.ARCHIVED
                && channel.getArchivedAt() != null
                && channel.getArchiveExpiresAt() != null
                && LocalDateTime.now().isBefore(channel.getArchiveExpiresAt());
    }

    // 요청한 메시지가 저장된 읽음 위치보다 뒤에 있을 때만 커서를 전진시킨다.
    private void advanceReadState(ChannelReadState readState, Message requestedMessage) {
        UUID currentMessageId = readState.getLastReadMessageId();
        if (currentMessageId == null || currentMessageId.equals(requestedMessage.getId())) {
            if (currentMessageId == null) {
                readState.advanceTo(requestedMessage.getId());
            }
            return;
        }

        messageRepository.findById(currentMessageId)
                .filter(currentMessage -> isAfter(requestedMessage, currentMessage))
                .ifPresent(ignored -> readState.advanceTo(requestedMessage.getId()));
    }

    private boolean isAfter(Message source, Message target) {
        int createdAtComparison = source.getCreatedAt().compareTo(target.getCreatedAt());
        return createdAtComparison > 0
                || (createdAtComparison == 0 && source.getId().compareTo(target.getId()) > 0);
    }

    // 생성자는 요청 누락과 무관하게 비공개 채널의 초기 멤버에 항상 포함한다.
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

    private void validateTargetWorkspaceMember(UUID workspaceId, UUID memberId) {
        WorkspaceMembership membership = workspaceMembershipRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (!membership.getWorkspaceId().equals(workspaceId)
                || membership.getStatus() != WorkspaceMembershipStatus.ACTIVE) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    // 과거 탈퇴 이력이 있으면 새 행을 만들지 않고 기존 참여 상태를 다시 활성화한다.
    private void addOrReactivatePrivateChannelMember(UUID channelId, UUID workspaceMembershipId) {
        channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channelId, workspaceMembershipId)
                .ifPresentOrElse(channelMembership -> {
                    if (channelMembership.getStatus() == ChannelMembershipStatus.LEFT) {
                        channelMembership.rejoin();
                        return;
                    }
                    if (channelMembership.getStatus() == ChannelMembershipStatus.REMOVED) {
                        throw new BusinessException(ChannelErrorCode.CHANNEL_MEMBER_REMOVED);
                    }
                }, () -> channelMembershipRepository.save(
                        ChannelMembership.join(channelId, workspaceMembershipId)
                ));
    }

    private boolean isPrivateChannelCreator(Channel channel, UUID workspaceMembershipId) {
        return channel.getVisibility() == ChannelVisibility.PRIVATE
                && channel.getCreatedByMembershipId().equals(workspaceMembershipId);
    }
}
