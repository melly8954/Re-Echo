package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 채널 멤버십 영속성 접근을 담당한다.
public interface ChannelMembershipRepository extends JpaRepository<ChannelMembership, UUID> {

    @Query("""
            select channelMembership.channelId
            from ChannelMembership channelMembership
            where channelMembership.workspaceMembershipId = :workspaceMembershipId
              and channelMembership.status = :status
            """)
    List<UUID> findChannelIdsByWorkspaceMembershipIdAndStatus(
            @Param("workspaceMembershipId") UUID workspaceMembershipId,
            @Param("status") ChannelMembershipStatus status
    );
}
