package com.reecho.reechobe.message.service;

import com.reecho.reechobe.file.domain.FileObject;
import com.reecho.reechobe.file.repository.FileObjectRepository;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.message.domain.Message;
import com.reecho.reechobe.message.domain.MessageAttachment;
import com.reecho.reechobe.message.dto.ChannelMessageResponse;
import com.reecho.reechobe.message.dto.MessageAttachmentResponse;
import com.reecho.reechobe.message.dto.MessageAuthorResponse;
import com.reecho.reechobe.message.repository.MessageAttachmentRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 메시지 목록과 단건 응답에 필요한 작성자·첨부 정보를 한 번에 조합한다.
@Component
@RequiredArgsConstructor
public class MessageResponseAssembler {

    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final FileObjectRepository fileObjectRepository;

    public List<ChannelMessageResponse> assembleAll(List<Message> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Map<UUID, WorkspaceMembership> authors = workspaceMembershipRepository.findAllById(
                        messages.stream().map(Message::getAuthorMembershipId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(WorkspaceMembership::getId, Function.identity()));
        Map<UUID, List<MessageAttachment>> attachments = messageAttachmentRepository
                .findByMessageIdInOrderByMessageIdAscSortOrderAsc(messages.stream().map(Message::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(MessageAttachment::getMessageId));
        Map<UUID, FileObject> files = fileObjectRepository.findAllById(
                        attachments.values().stream()
                                .flatMap(Collection::stream)
                                .map(MessageAttachment::getFileObjectId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(FileObject::getId, Function.identity()));
        return messages.stream()
                .map(message -> assemble(message, authors, attachments, files))
                .toList();
    }

    public ChannelMessageResponse assemble(Message message) {
        return assembleAll(List.of(message)).getFirst();
    }

    private ChannelMessageResponse assemble(
            Message message,
            Map<UUID, WorkspaceMembership> authors,
            Map<UUID, List<MessageAttachment>> attachments,
            Map<UUID, FileObject> files
    ) {
        WorkspaceMembership author = authors.get(message.getAuthorMembershipId());
        List<MessageAttachmentResponse> attachmentResponses = attachments
                .getOrDefault(message.getId(), List.of())
                .stream()
                .map(MessageAttachment::getFileObjectId)
                .map(files::get)
                .filter(fileObject -> fileObject != null && !fileObject.isDeleted())
                .map(MessageAttachmentResponse::from)
                .toList();
        return ChannelMessageResponse.of(message, MessageAuthorResponse.from(author), attachmentResponses);
    }
}
