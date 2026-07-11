package com.reecho.reechobe.workspace.repository;

import com.reecho.reechobe.workspace.domain.Workspace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 워크스페이스 영속성 접근을 담당한다.
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
}
