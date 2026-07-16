package com.reecho.reechobe.message.repository;

import com.reecho.reechobe.message.domain.MessageAttachment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 메시지 첨부 파일의 연결과 표시 순서 조회를 담당한다.
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageIdInOrderByMessageIdAscSortOrderAsc(Collection<UUID> messageIds);

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM message_attachments ma
                        JOIN messages m ON m.id = ma.message_id
                        JOIN channels c ON c.id = m.channel_id
                        JOIN channel_memberships cm ON cm.channel_id = c.id
                        WHERE ma.file_object_id = :fileId
                          AND m.status = 'ACTIVE'
                          AND c.status IN ('ACTIVE', 'ARCHIVED')
                          AND cm.workspace_membership_id = :workspaceMembershipId
                          AND cm.status = 'ACTIVE'
                    )
                    """,
            nativeQuery = true
    )
    boolean existsAccessibleMessageAttachment(
            @Param("fileId") UUID fileId,
            @Param("workspaceMembershipId") UUID workspaceMembershipId
    );
}
