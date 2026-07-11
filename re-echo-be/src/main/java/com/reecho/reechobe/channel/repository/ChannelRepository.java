package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.Channel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 채널 영속성 접근을 담당한다.
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
}
