package com.reecho.reechobe.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import java.util.UUID;

// 채널 목록에서 사이드바 표시에 필요한 최소 정보를 전달한다.
public record ChannelListItemResponse(
        UUID id,
        String name,
        ChannelVisibility visibility,
        @JsonProperty("isGeneral") boolean general,
        boolean joined,
        boolean createdByMe,
        int unreadCount
) {

    public static ChannelListItemResponse from(Channel channel, boolean joined, UUID membershipId) {
        return new ChannelListItemResponse(
                channel.getId(),
                channel.getName(),
                channel.getVisibility(),
                channel.isGeneral(),
                joined,
                channel.getCreatedByMembershipId().equals(membershipId),
                0
        );
    }
}
