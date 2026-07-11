package com.reecho.reechobe.member.repository;

import com.reecho.reechobe.member.domain.WorkspaceMembership;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

// 워크스페이스 멤버십 영속성 접근을 담당한다.
public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {
}
