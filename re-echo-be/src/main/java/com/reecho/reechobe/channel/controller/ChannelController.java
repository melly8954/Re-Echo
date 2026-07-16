package com.reecho.reechobe.channel.controller;

import com.reecho.reechobe.channel.dto.AddChannelMembersRequest;
import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.dto.CreateChannelRequest;
import com.reecho.reechobe.channel.dto.CreatedChannelResponse;
import com.reecho.reechobe.channel.dto.UpdateChannelReadStateRequest;
import com.reecho.reechobe.channel.service.command.ChannelCommandService;
import com.reecho.reechobe.channel.service.query.ChannelQueryService;
import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.member.dto.WorkspaceMemberListResponse;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 범위의 채널 생성과 조회 API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/channels")
public class ChannelController {

    private final ChannelCommandService channelCommandService;
    private final ChannelQueryService channelQueryService;

    @GetMapping
    // 현재 워크스페이스에서 접근 가능한 채널 목록 조회를 Query Service에 위임한다.
    public ApiResponse<ChannelListResponse> getChannels(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        ChannelListResponse result = channelQueryService.getChannels(principal.userId(), workspaceId);
        return ApiResponse.success(HttpStatus.OK, "채널 목록을 조회했습니다.", result);
    }

    @GetMapping("/{channelId}/members")
    // 채널 멤버 목록 조회와 비공개 채널 접근 검증을 Query Service에 위임한다.
    public ApiResponse<WorkspaceMemberListResponse> getChannelMembers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId
    ) {
        WorkspaceMemberListResponse result = channelQueryService.getChannelMembers(
                principal.userId(),
                workspaceId,
                channelId
        );
        return ApiResponse.success(HttpStatus.OK, "채널 멤버 목록을 조회했습니다.", result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // 채널 생성 요청을 Command Service에 위임하고 생성 채널 식별자를 반환한다.
    public ApiResponse<CreatedChannelResponse> createChannel(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        CreatedChannelResponse result = channelCommandService.createChannel(
                principal.userId(),
                workspaceId,
                request
        );
        return ApiResponse.success(HttpStatus.CREATED, "채널이 생성되었습니다.", result);
    }

    @PostMapping("/{channelId}/join")
    // 공개 채널 참여 요청을 Command Service에 위임한다.
    public ApiResponse<Void> joinPublicChannel(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId
    ) {
        channelCommandService.joinPublicChannel(principal.userId(), workspaceId, channelId);
        return ApiResponse.success(HttpStatus.OK, "채널에 참여했습니다.", null);
    }

    @PostMapping("/{channelId}/members")
    // 비공개 채널 멤버 추가 요청을 Command Service에 위임한다.
    public ApiResponse<Void> addPrivateChannelMembers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @Valid @RequestBody AddChannelMembersRequest request
    ) {
        channelCommandService.addPrivateChannelMembers(principal.userId(), workspaceId, channelId, request);
        return ApiResponse.success(HttpStatus.OK, "비공개 채널 멤버를 추가했습니다.", null);
    }

    @PostMapping("/{channelId}/leave")
    // 현재 사용자의 채널 탈퇴 요청을 Command Service에 위임한다.
    public ApiResponse<Void> leaveChannel(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId
    ) {
        channelCommandService.leaveChannel(principal.userId(), workspaceId, channelId);
        return ApiResponse.success(HttpStatus.OK, "채널에서 나갔습니다.", null);
    }

    @PutMapping("/{channelId}/read-state")
    // 채널별 마지막 읽음 메시지 갱신 요청을 Command Service에 위임한다.
    public ApiResponse<Void> updateChannelReadState(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @Valid @RequestBody UpdateChannelReadStateRequest request
    ) {
        channelCommandService.updateChannelReadState(principal.userId(), workspaceId, channelId, request);
        return ApiResponse.success(HttpStatus.OK, "채널 읽음 상태를 갱신했습니다.", null);
    }

    @DeleteMapping("/{channelId}/members/{memberId}")
    // 비공개 채널 멤버 제거 요청을 Command Service에 위임한다.
    public ApiResponse<Void> removeChannelMember(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId,
            @PathVariable UUID memberId
    ) {
        channelCommandService.removeChannelMember(principal.userId(), workspaceId, channelId, memberId);
        return ApiResponse.success(HttpStatus.OK, "채널 멤버를 제거했습니다.", null);
    }
}
