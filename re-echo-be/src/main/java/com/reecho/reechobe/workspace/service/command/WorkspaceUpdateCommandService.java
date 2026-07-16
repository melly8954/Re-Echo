package com.reecho.reechobe.workspace.service.command;

import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.channel.repository.ChannelRepository;
import com.reecho.reechobe.file.service.WorkspaceImageFileService;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.workspace.domain.Workspace;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.dto.UpdateWorkspaceRequest;
import com.reecho.reechobe.workspace.dto.WorkspaceDetailResponse;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 권한자가 워크스페이스 정보와 대표 이미지를 함께 변경하도록 처리한다.
@Service
@RequiredArgsConstructor
public class WorkspaceUpdateCommandService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ChannelRepository channelRepository;
    private final WorkspaceImageFileService workspaceImageFileService;

    @Transactional
    // 소유자 또는 관리자가 기본 정보와 대표 이미지를 한 트랜잭션으로 갱신한다.
    public WorkspaceDetailResponse updateWorkspace(
            UUID userId,
            UUID workspaceId,
            UpdateWorkspaceRequest request
    ) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspace.getStatus() == WorkspaceStatus.DELETED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        }
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        userId,
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        if (!canManageWorkspace(membership)) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        if (membership.getRole() != WorkspaceMembershipRole.OWNER
                && !workspace.getName().equals(request.name().trim())) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }

        UUID previousImageFileId = workspace.getImageFileId();
        String imageUrl = request.imageFileId() == null
                ? null
                : workspaceImageFileService.requireUploadedWorkspaceImageUrl(
                        workspaceId,
                        userId,
                        request.imageFileId()
                );
        workspace.update(request.name(), request.description(), request.imageFileId(), imageUrl);

        if (previousImageFileId != null && !previousImageFileId.equals(request.imageFileId())) {
            workspaceImageFileService.markWorkspaceImageOrphaned(workspaceId, previousImageFileId);
        }

        return WorkspaceDetailResponse.of(
                workspace,
                membership,
                channelRepository.findByWorkspaceIdAndGeneralTrue(workspaceId)
                        .orElseThrow(() -> new IllegalStateException("기본 채널이 없습니다."))
                        .getId(),
                false
        );
    }

    // 이름을 제외한 워크스페이스 설정 변경에는 소유자와 관리자만 허용한다.
    private boolean canManageWorkspace(WorkspaceMembership membership) {
        return membership.getRole() == WorkspaceMembershipRole.OWNER
                || membership.getRole() == WorkspaceMembershipRole.ADMIN;
    }
}
