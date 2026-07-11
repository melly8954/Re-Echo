package com.reecho.reechobe.workspace.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import com.reecho.reechobe.workspace.dto.CreateWorkspaceRequest;
import com.reecho.reechobe.workspace.dto.CreatedWorkspaceResponse;
import com.reecho.reechobe.workspace.dto.WorkspaceDetailResponse;
import com.reecho.reechobe.workspace.dto.WorkspaceListResponse;
import com.reecho.reechobe.workspace.service.command.WorkspaceCreateCommandService;
import com.reecho.reechobe.workspace.service.query.WorkspaceQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 생성과 조회 API의 진입점을 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceCreateCommandService workspaceCreateCommandService;
    private final WorkspaceQueryService workspaceQueryService;

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
}
