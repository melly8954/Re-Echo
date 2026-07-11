package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.ChannelMembership;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 채널 멤버십 영속성 접근을 담당한다.
public interface ChannelMembershipRepository extends JpaRepository<ChannelMembership, UUID> {
}
