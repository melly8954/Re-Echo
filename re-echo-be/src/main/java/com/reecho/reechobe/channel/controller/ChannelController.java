package com.reecho.reechobe.channel.controller;

import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.dto.CreateChannelRequest;
import com.reecho.reechobe.channel.dto.CreatedChannelResponse;
import com.reecho.reechobe.channel.service.command.ChannelCommandService;
import com.reecho.reechobe.channel.service.query.ChannelQueryService;
import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ApiResponse<ChannelListResponse> getChannels(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        ChannelListResponse result = channelQueryService.getChannels(principal.userId(), workspaceId);
        return ApiResponse.success(HttpStatus.OK, "채널 목록을 조회했습니다.", result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
    public ApiResponse<Void> joinPublicChannel(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId
    ) {
        channelCommandService.joinPublicChannel(principal.userId(), workspaceId, channelId);
        return ApiResponse.success(HttpStatus.OK, "채널에 참여했습니다.", null);
    }

    @PostMapping("/{channelId}/leave")
    public ApiResponse<Void> leavePublicChannel(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID channelId
    ) {
        channelCommandService.leavePublicChannel(principal.userId(), workspaceId, channelId);
        return ApiResponse.success(HttpStatus.OK, "채널에서 나갔습니다.", null);
    }
}
