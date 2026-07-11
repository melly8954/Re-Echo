package com.reecho.reechobe.channel.controller;

import com.reecho.reechobe.channel.dto.ChannelListResponse;
import com.reecho.reechobe.channel.service.query.ChannelQueryService;
import com.reecho.reechobe.common.response.ApiResponse;
import com.reecho.reechobe.security.AuthenticatedUserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 워크스페이스 범위의 채널 조회 API를 제공한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/channels")
public class ChannelController {

    private final ChannelQueryService channelQueryService;

    @GetMapping
    public ApiResponse<ChannelListResponse> getChannels(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID workspaceId
    ) {
        ChannelListResponse result = channelQueryService.getChannels(principal.userId(), workspaceId);
        return ApiResponse.success(HttpStatus.OK, "채널 목록을 조회했습니다.", result);
    }
}
