package com.reecho.reechobe.invite.repository;

import com.reecho.reechobe.invite.domain.WorkspaceInviteLink;
import com.reecho.reechobe.invite.domain.WorkspaceInviteLinkStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 초대 링크 영속성 접근을 담당한다.
public interface WorkspaceInviteLinkRepository extends JpaRepository<WorkspaceInviteLink, UUID> {

    Optional<WorkspaceInviteLink> findByWorkspaceIdAndStatus(
            UUID workspaceId,
            WorkspaceInviteLinkStatus status
    );

    Optional<WorkspaceInviteLink> findByToken(String token);

    boolean existsByToken(String token);
}
