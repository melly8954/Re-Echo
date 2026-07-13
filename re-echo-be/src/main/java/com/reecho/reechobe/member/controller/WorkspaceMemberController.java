package com.reecho.reechobe.member.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.member.dto.WorkspaceMemberListResponse;
import com.reecho.reechobe.member.service.query.WorkspaceMemberQueryService;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 멤버 조회 API의 진입점을 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberQueryService workspaceMemberQueryService;

    @GetMapping
    public ApiResponse<WorkspaceMemberListResponse> getWorkspaceMembers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        WorkspaceMemberListResponse result = workspaceMemberQueryService.getWorkspaceMembers(
                principal.userId(),
                workspaceId
        );
        return ApiResponse.success(HttpStatus.OK, "워크스페이스 멤버 목록을 조회했습니다.", result);
    }
}
