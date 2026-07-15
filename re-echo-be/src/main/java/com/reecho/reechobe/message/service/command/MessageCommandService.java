package com.reecho.reechobe.message.service.command;

import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.exception.ChannelErrorCode;
import com.reecho.reechobe.channel.repository.ChannelMembershipRepository;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.domain.FilePurpose;
import com.reecho.reechobe.file.domain.FileStatus;
import com.reecho.reechobe.file.exception.FileErrorCode;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.message.domain.Message;
import com.reecho.reechobe.message.domain.MessageAttachment;
import com.reecho.reechobe.message.dto.ChannelMessageResponse;
import com.reecho.reechobe.message.dto.CreateMessageRequest;
import com.reecho.reechobe.message.dto.UpdateMessageRequest;
import com.reecho.reechobe.message.exception.MessageErrorCode;
import com.reecho.reechobe.message.repository.MessageAttachmentRepository;
import com.reecho.reechobe.message.repository.MessageRepository;
import com.reecho.reechobe.message.service.MessageResponseAssembler;
import com.reecho.reechobe.realtime.dto.RealtimeEventType;
import com.reecho.reechobe.realtime.service.RealtimeEventPublisher;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 채널 멤버의 메시지 작성·수정·삭제와 첨부 연결을 트랜잭션으로 처리한다.
@Service
@RequiredArgsConstructor
public class MessageCommandService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMembershipRepository channelMembershipRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final FileObjectRepository fileObjectRepository;
    private final MessageResponseAssembler messageResponseAssembler;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Transactional
    public ChannelMessageResponse createMessage(
            UUID userId,
            UUID workspaceId,
            UUID channelId,
            CreateMessageRequest request
    ) {
        WorkspaceMembership membership = validateWritableChannel(userId, workspaceId, channelId);
        String content = normalizeContent(request.content());
        List<UUID> fileIds = normalizeFileIds(request.fileIds());
        validateMessageInput(content, fileIds);
        validateAttachableFiles(workspaceId, membership.getId(), fileIds);
        Message message = messageRepository.save(Message.create(channelId, membership.getId(), content));
        saveAttachments(message.getId(), fileIds);
        ChannelMessageResponse response = messageResponseAssembler.assemble(message);
        publishMessageAfterCommit(workspaceId, RealtimeEventType.MESSAGE_CREATED, response);
        return response;
    }

    @Transactional
    public ChannelMessageResponse updateMessage(
            UUID userId,
            UUID workspaceId,
            UUID channelId,
            UUID messageId,
            UpdateMessageRequest request
    ) {
        WorkspaceMembership membership = validateWritableChannel(userId, workspaceId, channelId);
        Message message = getActiveMessage(channelId, messageId);
        if (!message.getAuthorMembershipId().equals(membership.getId())) {
            throw new BusinessException(MessageErrorCode.MESSAGE_EDIT_FORBIDDEN);
        }
        String content = normalizeContent(request.content());
        List<UUID> fileIds = normalizeFileIds(request.fileIds());
        validateMessageInput(content, fileIds);
        validateAttachableFiles(workspaceId, membership.getId(), fileIds);
        message.edit(content);
        messageAttachmentRepository.deleteAll(
                messageAttachmentRepository.findByMessageIdInOrderByMessageIdAscSortOrderAsc(List.of(message.getId()))
        );
        saveAttachments(message.getId(), fileIds);
        ChannelMessageResponse response = messageResponseAssembler.assemble(message);
        publishMessageAfterCommit(workspaceId, RealtimeEventType.MESSAGE_UPDATED, response);
        return response;
    }

    @Transactional
    public void deleteMessage(UUID userId, UUID workspaceId, UUID channelId, UUID messageId) {
        WorkspaceMembership membership = validateWritableChannel(userId, workspaceId, channelId);
        Message message = getActiveMessage(channelId, messageId);
        boolean canDelete = message.getAuthorMembershipId().equals(membership.getId())
                || membership.getRole() == WorkspaceMembershipRole.OWNER
                || membership.getRole() == WorkspaceMembershipRole.ADMIN;
        if (!canDelete) {
            throw new BusinessException(MessageErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        message.delete(membership.getId());
        publishMessageAfterCommit(
                workspaceId,
                RealtimeEventType.MESSAGE_DELETED,
                messageResponseAssembler.assemble(message)
        );
    }

    private WorkspaceMembership validateWritableChannel(UUID userId, UUID workspaceId, UUID channelId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .filter(foundWorkspace -> foundWorkspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(workspaceId, userId, WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        Channel channel = channelRepository.findById(channelId)
                .filter(foundChannel -> foundChannel.getWorkspaceId().equals(workspaceId))
                .filter(foundChannel -> foundChannel.getStatus() != ChannelStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_NOT_FOUND));
        if (channel.getStatus() == ChannelStatus.ARCHIVED) {
            throw new BusinessException(ChannelErrorCode.CHANNEL_ARCHIVED);
        }
        channelMembershipRepository.findByChannelIdAndWorkspaceMembershipId(channelId, membership.getId())
                .filter(channelMembership -> channelMembership.getStatus() == ChannelMembershipStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ChannelErrorCode.CHANNEL_ACCESS_DENIED));
        return membership;
    }

    private Message getActiveMessage(UUID channelId, UUID messageId) {
        return messageRepository.findById(messageId)
                .filter(message -> message.getChannelId().equals(channelId))
                .filter(message -> !message.isDeleted())
                .orElseThrow(() -> new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND));
    }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.trim();
    }

    private List<UUID> normalizeFileIds(List<UUID> fileIds) {
        return fileIds == null ? List.of() : List.copyOf(fileIds);
    }

    private void validateMessageInput(String content, List<UUID> fileIds) {
        if (content == null && fileIds.isEmpty()) {
            throw new BusinessException(MessageErrorCode.MESSAGE_EMPTY_CONTENT);
        }
        if (new HashSet<>(fileIds).size() != fileIds.size()) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED, "첨부 파일은 중복할 수 없습니다.");
        }
    }

    private void validateAttachableFiles(UUID workspaceId, UUID membershipId, List<UUID> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        Map<UUID, FileObject> files = fileObjectRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileObject::getId, Function.identity()));
        if (files.size() != fileIds.size()) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
        boolean invalidFile = files.values().stream().anyMatch(fileObject ->
                !workspaceId.equals(fileObject.getWorkspaceId())
                        || !membershipId.equals(fileObject.getUploadedByMembershipId())
                        || fileObject.getPurpose() != FilePurpose.MESSAGE_ATTACHMENT
                        || fileObject.getStatus() != FileStatus.ACTIVE
        );
        if (invalidFile) {
            throw new BusinessException(FileErrorCode.FILE_ACCESS_DENIED);
        }
    }

    private void saveAttachments(UUID messageId, List<UUID> fileIds) {
        for (int index = 0; index < fileIds.size(); index++) {
            messageAttachmentRepository.save(MessageAttachment.create(messageId, fileIds.get(index), index));
        }
    }

    // 수신자가 커밋 전 데이터를 다시 읽지 않도록 완료 후에만 event를 전파한다.
    private void publishMessageAfterCommit(
            UUID workspaceId,
            RealtimeEventType type,
            ChannelMessageResponse response
    ) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimeEventPublisher.publishMessage(workspaceId, type, response);
            }
        });
    }
}
