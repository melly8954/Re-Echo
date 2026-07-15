package com.reecho.reechobe.message.service.query;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.common.response.CursorPageInfo;
import com.reecho.reechobe.common.response.CursorPageResponse;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.message.domain.Message;
import com.reecho.reechobe.message.dto.ChannelMessageResponse;
import com.reecho.reechobe.message.dto.MessageCursorResponse;
import com.reecho.reechobe.message.repository.MessageRepository;
import com.reecho.reechobe.message.service.MessageResponseAssembler;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 채널 멤버만 최신 메시지와 과거 메시지를 cursor 기준으로 조회한다.
@Service
@RequiredArgsConstructor
public class MessageQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;
    private final MessageRepository messageRepository;
    private final MessageResponseAssembler messageResponseAssembler;

    @Transactional(readOnly = true)
    public CursorPageResponse<ChannelMessageResponse, MessageCursorResponse> getMessages(
            UUID userId,
            UUID workspaceId,
            UUID channelId,
            int size,
            LocalDateTime cursorCreatedAt,
            UUID cursorMessageId
    ) {
        validateCursor(cursorCreatedAt, cursorMessageId);
        WorkspaceMembership membership = validateReadableChannel(userId, workspaceId, channelId);
        List<Message> queriedMessages = cursorCreatedAt == null
                ? messageRepository.findByChannelIdOrderByCreatedAtDescIdDesc(channelId, PageRequest.of(0, size + 1))
                : messageRepository
                        .findByChannelIdAndCreatedAtLessThanOrChannelIdAndCreatedAtAndIdLessThanOrderByCreatedAtDescIdDesc(
                                channelId,
                                cursorCreatedAt,
                                channelId,
                                cursorCreatedAt,
                                cursorMessageId,
                                PageRequest.of(0, size + 1)
                        );
        boolean hasNext = queriedMessages.size() > size;
        List<Message> messages = hasNext ? queriedMessages.subList(0, size) : queriedMessages;
        MessageCursorResponse nextCursor = hasNext
                ? toCursor(messages.getLast())
                : null;
        return new CursorPageResponse<>(
                messageResponseAssembler.assembleAll(messages),
                CursorPageInfo.of(size, hasNext, nextCursor)
        );
    }

    private WorkspaceMembership validateReadableChannel(UUID userId, UUID workspaceId, UUID channelId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(workspace.getId(), userId, WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        Channel channel = channelRepository.findById(channelId)
                .filter(foundChannel -> foundChannel.getWorkspaceId().equals(workspaceId))
                .filter(foundChannel -> foundChannel.getStatus() != ChannelStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND));
        channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channel.getId(), membership.getId())
                .filter(channelMembership -> channelMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
        return membership;
    }

    private void validateCursor(LocalDateTime cursorCreatedAt, UUID cursorMessageId) {
        if ((cursorCreatedAt == null) != (cursorMessageId == null)) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED, "메시지 cursor 값이 올바르지 않습니다.");
        }
    }

    private MessageCursorResponse toCursor(Message message) {
        return new MessageCursorResponse(message.getCreatedAt(), message.getId());
    }
}
