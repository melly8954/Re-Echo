package com.reecho.reechobe.member.repository;

import com.reecho.reechobe.channel.domain.ChannelMembershipStatus;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 워크스페이스 멤버십 영속성 접근을 담당한다.
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserIdAndStatus(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipStatus status
    );

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    Optional<WorkspaceMembership> findByIdAndWorkspaceIdAndStatus(
            UUID id,
            UUID workspaceId,
            WorkspaceMembershipStatus status
    );

    List<WorkspaceMembership> findByUserIdAndStatusOrderByJoinedAtAscIdAsc(
            UUID userId,
            WorkspaceMembershipStatus status
    );

    List<WorkspaceMembership> findByWorkspaceIdAndStatus(
            UUID workspaceId,
            WorkspaceMembershipStatus status
    );

    @Query("""
            select membership
            from WorkspaceMembership membership
            where membership.status = :membershipStatus
              and membership.id in (
                  select channelMembership.workspaceMembershipId
                  from ChannelMembership channelMembership
                  where channelMembership.channelId = :channelId
                    and channelMembership.status = :channelMembershipStatus
              )
            """)
    List<WorkspaceMembership> findActiveChannelMembers(
            @Param("channelId") UUID channelId,
            @Param("membershipStatus") WorkspaceMembershipStatus membershipStatus,
            @Param("channelMembershipStatus") ChannelMembershipStatus channelMembershipStatus
    );
}
