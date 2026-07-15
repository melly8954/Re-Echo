package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.ChannelReadState;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 채널 멤버십별 읽음 위치 영속성 접근을 담당한다.
public interface ChannelReadStateRepository extends JpaRepository<ChannelReadState, UUID> {

    Optional<ChannelReadState> findByChannelMembershipId(UUID channelMembershipId);
}
