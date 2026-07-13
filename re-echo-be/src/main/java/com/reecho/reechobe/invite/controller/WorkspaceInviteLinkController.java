package com.reecho.reechobe.invite.controller;

import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.invite.dto.JoinedWorkspaceResponse;
import com.reecho.reechobe.invite.dto.WorkspaceInviteLinkResponse;
import com.reecho.reechobe.invite.dto.WorkspaceInvitePreviewResponse;
import com.reecho.reechobe.invite.service.command.WorkspaceInviteLinkCommandService;
import com.reecho.reechobe.invite.service.query.WorkspaceInviteLinkQueryService;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 초대 링크 발급, 조회, 참여 API를 제공한다.
@RestController
@RequiredArgsConstructor
public class WorkspaceInviteLinkController {

    private final WorkspaceInviteLinkCommandService commandService;
    private final WorkspaceInviteLinkQueryService queryService;

    @GetMapping("/api/v1/workspaces/{workspaceId}/invite-link")
    public ApiResponse<WorkspaceInviteLinkResponse> getActiveInviteLink(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        WorkspaceInviteLinkResponse result = queryService.getActiveInviteLink(
                principal.userId(),
                workspaceId
        );
        return ApiResponse.success(HttpStatus.OK, "초대 링크를 조회했습니다.", result);
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/invite-link")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkspaceInviteLinkResponse> issueInviteLink(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        WorkspaceInviteLinkResponse result = commandService.issueInviteLink(
                principal.userId(),
                workspaceId
        );
        return ApiResponse.success(HttpStatus.CREATED, "초대 링크를 발급했습니다.", result);
    }

    @GetMapping("/api/v1/invite-links/{token}")
    public ApiResponse<WorkspaceInvitePreviewResponse> previewInviteLink(
            @PathVariable String token
    ) {
        WorkspaceInvitePreviewResponse result = queryService.previewInviteLink(token);
        return ApiResponse.success(HttpStatus.OK, "초대 링크를 조회했습니다.", result);
    }

    @PostMapping("/api/v1/invite-links/{token}/join")
    public ApiResponse<JoinedWorkspaceResponse> joinWorkspace(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String token
    ) {
        JoinedWorkspaceResponse result = commandService.joinWorkspace(principal.userId(), token);
        return ApiResponse.success(HttpStatus.OK, "워크스페이스에 참여했습니다.", result);
    }
}
