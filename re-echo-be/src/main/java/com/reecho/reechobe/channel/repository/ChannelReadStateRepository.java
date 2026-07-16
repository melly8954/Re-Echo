package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.ChannelReadState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 채널 멤버십별 읽음 위치 영속성 접근을 담당한다.
public interface ChannelReadStateRepository extends JpaRepository<ChannelReadState, UUID> {

    Optional<ChannelReadState> findByChannelMembershipId(UUID channelMembershipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT readState FROM ChannelReadState readState WHERE readState.channelMembershipId = :channelMembershipId")
    Optional<ChannelReadState> findByChannelMembershipIdForUpdate(
            @Param("channelMembershipId") UUID channelMembershipId
    );
}
