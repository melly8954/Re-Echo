package com.reecho.reechobe.channel.repository;

import com.reecho.reechobe.channel.domain.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 채널 영속성 접근을 담당한다.
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    Optional<Channel> findByWorkspaceIdAndGeneralTrue(UUID workspaceId);

    @Query(
            value = """
                    SELECT c.*
                    FROM channels c
                    WHERE c.workspace_id = :workspaceId
                      AND c.status = 'ACTIVE'
                      AND (
                          c.visibility = 'PUBLIC'
                          OR EXISTS (
                              SELECT 1
                              FROM channel_memberships cm
                              WHERE cm.channel_id = c.id
                                AND cm.workspace_membership_id = :workspaceMembershipId
                                AND cm.status = 'ACTIVE'
                          )
                      )
                    ORDER BY
                        CASE WHEN c.is_general THEN 0 ELSE 1 END,
                        COALESCE(
                            (
                                SELECT MAX(m.created_at)
                                FROM messages m
                                WHERE m.channel_id = c.id
                                  AND m.status = 'ACTIVE'
                            ),
                            c.created_at
                        ) DESC
                    """,
            nativeQuery = true
    )
    List<Channel> findAccessibleActiveChannels(
            @Param("workspaceId") UUID workspaceId,
            @Param("workspaceMembershipId") UUID workspaceMembershipId
    );
}
