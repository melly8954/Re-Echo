package com.reecho.reechobe.member.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.member.dto.ChangeWorkspaceMemberRoleRequest;
import com.reecho.reechobe.member.dto.WorkspaceMemberListResponse;
import jakarta.validation.Valid;
import com.reecho.reechobe.member.service.command.WorkspaceMemberCommandService;
import com.reecho.reechobe.member.service.query.WorkspaceMemberQueryService;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 멤버 조회 API의 진입점을 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberCommandService workspaceMemberCommandService;
    private final WorkspaceMemberQueryService workspaceMemberQueryService;

    @GetMapping
    // 워크스페이스 멤버 목록 조회를 Query Service에 위임한다.
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

    @PatchMapping("/{memberId}/role")
    // 멤버 역할 변경 요청을 Command Service에 위임한다.
    public ApiResponse<Void> changeMemberRole(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody ChangeWorkspaceMemberRoleRequest request
    ) {
        workspaceMemberCommandService.changeMemberRole(
                principal.userId(),
                workspaceId,
                memberId,
                request.role()
        );
        return ApiResponse.success(HttpStatus.OK, "멤버 역할을 변경했습니다.", null);
    }

    @PostMapping("/{memberId}/remove")
    // 멤버 강제 제거 요청을 Command Service에 위임한다.
    public ApiResponse<Void> removeMember(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId
    ) {
        workspaceMemberCommandService.removeMember(principal.userId(), workspaceId, memberId);
        return ApiResponse.success(HttpStatus.OK, "멤버를 워크스페이스에서 제거했습니다.", null);
    }

    @PostMapping("/{memberId}/leave")
    // 현재 사용자 워크스페이스 탈퇴 요청을 Command Service에 위임한다.
    public ApiResponse<Void> leaveWorkspace(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId
    ) {
        workspaceMemberCommandService.leaveWorkspace(principal.userId(), workspaceId, memberId);
        return ApiResponse.success(HttpStatus.OK, "워크스페이스에서 나갔습니다.", null);
    }
}
