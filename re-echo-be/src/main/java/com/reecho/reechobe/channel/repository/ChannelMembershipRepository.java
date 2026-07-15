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

    @Query(
            value = """
                    SELECT m.channel_id AS "channelId",
                           COUNT(*) AS "unreadCount"
                    FROM messages m
                    JOIN channel_memberships cm ON cm.channel_id = m.channel_id
                    LEFT JOIN channel_read_states crs ON crs.channel_membership_id = cm.id
                    LEFT JOIN messages read_message ON read_message.id = crs.last_read_message_id
                    WHERE cm.workspace_membership_id = :workspaceMembershipId
                      AND cm.status = 'ACTIVE'
                      AND m.channel_id IN (:channelIds)
                      AND m.status = 'ACTIVE'
                      AND (
                          read_message.id IS NULL
                          OR m.created_at > read_message.created_at
                          OR (m.created_at = read_message.created_at AND m.id > read_message.id)
                      )
                    GROUP BY m.channel_id
                    """,
            nativeQuery = true
    )
    List<ChannelUnreadCountProjection> countUnreadActiveMessagesByChannelIds(
            @Param("workspaceMembershipId") UUID workspaceMembershipId,
            @Param("channelIds") List<UUID> channelIds
    );
}
