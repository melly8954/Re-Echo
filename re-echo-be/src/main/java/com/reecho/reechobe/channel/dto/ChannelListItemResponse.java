package com.reecho.reechobe.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import java.time.LocalDateTime;
import java.util.UUID;

// 채널 목록과 워크스페이스 홈 표시에 필요한 정보를 전달한다.
public record ChannelListItemResponse(
        UUID id,
        String name,
        ChannelVisibility visibility,
        @JsonProperty("isGeneral") boolean general,
        boolean joined,
        boolean createdByMe,
        ChannelStatus status,
        LocalDateTime archiveExpiresAt,
        int unreadCount,
        long memberCount
) {

    public static ChannelListItemResponse from(
            Channel channel,
            boolean joined,
            UUID membershipId,
            long memberCount,
            long unreadCount
    ) {
        return new ChannelListItemResponse(
                channel.getId(),
                channel.getName(),
                channel.getVisibility(),
                channel.isGeneral(),
                joined,
                channel.getCreatedByMembershipId().equals(membershipId),
                channel.getStatus(),
                channel.getArchiveExpiresAt(),
                Math.toIntExact(unreadCount),
                memberCount
        );
    }
}
