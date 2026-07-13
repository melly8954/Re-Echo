package com.reecho.reechobe.member.service.query;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.dto.WorkspaceMemberListResponse;
import com.reecho.reechobe.member.dto.WorkspaceMemberResponse;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 워크스페이스 멤버 목록 화면에 필요한 조회 모델을 구성한다.
@Service
@RequiredArgsConstructor
public class WorkspaceMemberQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;

    @Transactional(readOnly = true)
    public WorkspaceMemberListResponse getWorkspaceMembers(UUID userId, UUID workspaceId) {
        workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.getStatus() != WorkspaceStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));

        List<WorkspaceMemberResponse> members = workspaceMembershipRepository
                .findByWorkspaceIdAndStatus(workspaceId, WorkspaceMembershipStatus.ACTIVE)
                .stream()
                .sorted(Comparator
                        .comparingInt((WorkspaceMembership membership) -> roleOrder(membership.getRole()))
                        .thenComparing(WorkspaceMembership::getDisplayName))
                .map(WorkspaceMemberResponse::from)
                .toList();

        return WorkspaceMemberListResponse.of(members);
    }

    private int roleOrder(WorkspaceMembershipRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }
}
