package com.reecho.reechobe.channel.dto;

import java.util.List;

// 채널 목록 API의 공통 목록 응답 형태를 유지한다.
public record ChannelListResponse(List<ChannelListItemResponse> contents) {

    public static ChannelListResponse of(List<ChannelListItemResponse> contents) {
        return new ChannelListResponse(contents);
    }
}
