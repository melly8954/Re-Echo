package com.reecho.reechobe.workspace.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.common.exception.BusinessException;
import com.reecho.reechobe.file.dto.PresignedUploadResponse;
import com.reecho.reechobe.file.dto.ProfileImagePresignRequest;
import com.reecho.reechobe.file.service.WorkspaceImageFileService;
import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import com.reecho.reechobe.member.repository.WorkspaceMembershipRepository;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import com.reecho.reechobe.workspace.dto.CreateWorkspaceRequest;
import com.reecho.reechobe.workspace.dto.CreatedWorkspaceResponse;
import com.reecho.reechobe.workspace.dto.UpdateWorkspaceRequest;
import com.reecho.reechobe.workspace.dto.WorkspaceDetailResponse;
import com.reecho.reechobe.workspace.dto.WorkspaceListResponse;
import com.reecho.reechobe.workspace.domain.WorkspaceStatus;
import com.reecho.reechobe.workspace.exception.WorkspaceErrorCode;
import com.reecho.reechobe.workspace.repository.WorkspaceRepository;
import com.reecho.reechobe.workspace.service.command.WorkspaceCreateCommandService;
import com.reecho.reechobe.workspace.service.command.WorkspaceUpdateCommandService;
import com.reecho.reechobe.workspace.service.query.WorkspaceQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 생성, 조회, 설정 변경 API의 진입점을 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceCreateCommandService workspaceCreateCommandService;
    private final WorkspaceUpdateCommandService workspaceUpdateCommandService;
    private final WorkspaceQueryService workspaceQueryService;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceImageFileService workspaceImageFileService;

    @GetMapping
    public ApiResponse<WorkspaceListResponse> getWorkspaces(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        WorkspaceListResponse result = workspaceQueryService.getWorkspaceList(principal.userId());
        return ApiResponse.success(HttpStatus.OK, "워크스페이스 목록을 조회했습니다.", result);
    }

    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceDetailResponse> getWorkspace(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        WorkspaceDetailResponse result = workspaceQueryService.getWorkspaceDetail(
                principal.userId(),
                workspaceId
        );
        return ApiResponse.success(HttpStatus.OK, "워크스페이스를 조회합니다.", result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatedWorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        CreatedWorkspaceResponse result = workspaceCreateCommandService.createWorkspace(principal.userId(), request);
        return ApiResponse.success(HttpStatus.CREATED, "워크스페이스가 생성되었습니다.", result);
    }

    @PatchMapping("/{workspaceId}")
    public ApiResponse<WorkspaceDetailResponse> updateWorkspace(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        WorkspaceDetailResponse result = workspaceUpdateCommandService.updateWorkspace(
                principal.userId(),
                workspaceId,
                request
        );
        return ApiResponse.success(HttpStatus.OK, "워크스페이스 정보를 수정했습니다.", result);
    }

    @PostMapping("/{workspaceId}/image/presign-upload")
    public ApiResponse<PresignedUploadResponse> createWorkspaceImageUploadUrl(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ProfileImagePresignRequest request
    ) {
        WorkspaceStatus workspaceStatus = workspaceRepository.findById(workspaceId)
                .map(workspace -> workspace.getStatus())
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        if (workspaceStatus == WorkspaceStatus.DELETED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
        }
        if (workspaceStatus == WorkspaceStatus.ARCHIVED) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ARCHIVED);
        }
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        principal.userId(),
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
        if (membership.getRole() != WorkspaceMembershipRole.OWNER
                && membership.getRole() != WorkspaceMembershipRole.ADMIN) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        PresignedUploadResponse result = workspaceImageFileService.createWorkspaceImageUpload(
                workspaceId,
                principal.userId(),
                membership.getId(),
                request
        );
        return ApiResponse.success(HttpStatus.OK, "워크스페이스 대표 이미지 업로드 URL을 발급했습니다.", result);
    }
}
