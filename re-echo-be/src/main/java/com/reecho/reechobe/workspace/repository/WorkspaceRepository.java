package com.reecho.reechobe.workspace.repository;

import com.reecho.reechobe.workspace.domain.Workspace;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 워크스페이스 영속성 접근을 담당한다.
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT workspace FROM Workspace workspace WHERE workspace.id = :workspaceId")
    Optional<Workspace> findByIdForUpdate(@Param("workspaceId") UUID workspaceId);
}
