package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.ChannelMembership;
import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 채널 멤버십 영속성 접근을 담당한다.
public interface ChannelMembershipRepository extends JpaRepository<ChannelMembership, UUID> {

    Optional<ChannelMembership> findByChannelIdAndWorkspaceMembershipId(
            UUID channelId,
            UUID workspaceMembershipId
    );

    List<ChannelMembership> findByWorkspaceMembershipIdAndStatus(
            UUID workspaceMembershipId,
            ChannelMembershipStatus status
    );

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

    @Query("""
            select channelMembership.channelId as channelId,
                   count(channelMembership) as memberCount
            from ChannelMembership channelMembership
            where channelMembership.channelId in :channelIds
              and channelMembership.status = :status
            group by channelMembership.channelId
            """)
    List<ChannelMemberCountProjection> countByChannelIdsAndStatus(
            @Param("channelIds") List<UUID> channelIds,
            @Param("status") ChannelMembershipStatus status
    );
}
