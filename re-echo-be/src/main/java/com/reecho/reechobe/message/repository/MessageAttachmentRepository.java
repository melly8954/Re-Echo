package com.reecho.reechobe.message.repository;

import com.reecho.reechobe.message.domain.MessageAttachment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 메시지 첨부 파일의 연결과 표시 순서 조회를 담당한다.
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageIdInOrderByMessageIdAscSortOrderAsc(Collection<UUID> messageIds);
}
