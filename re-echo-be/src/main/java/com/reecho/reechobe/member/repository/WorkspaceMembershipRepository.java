package com.reecho.reechobe.member.repository;

import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 워크스페이스 멤버십 영속성 접근을 담당한다.
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserIdAndStatus(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipStatus status
    );

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<WorkspaceMembership> findByUserIdAndStatusOrderByLastVisitedAtDesc(
            UUID userId,
            WorkspaceMembershipStatus status
    );

    List<WorkspaceMembership> findByWorkspaceIdAndStatus(
            UUID workspaceId,
            WorkspaceMembershipStatus status
    );
}
