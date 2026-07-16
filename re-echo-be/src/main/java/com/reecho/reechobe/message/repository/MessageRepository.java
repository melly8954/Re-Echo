package com.reecho.reechobe.message.repository;

import com.reecho.reechobe.message.domain.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// 채널별 메시지 cursor 조회를 담당한다.
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChannelIdOrderByCreatedAtDescIdDesc(UUID channelId, Pageable pageable);

    List<Message> findByChannelIdAndCreatedAtLessThanOrChannelIdAndCreatedAtAndIdLessThanOrderByCreatedAtDescIdDesc(
            UUID channelId,
            LocalDateTime cursorCreatedAt,
            UUID cursorChannelId,
            LocalDateTime sameCreatedAt,
            UUID cursorMessageId,
            Pageable pageable
    );
}
