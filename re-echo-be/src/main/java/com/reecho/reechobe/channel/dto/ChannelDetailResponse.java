package com.reecho.reechobe.channel.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reecho.reechobe.channel.domain.Channel;
import com.reecho.reechobe.channel.domain.ChannelStatus;
import com.reecho.reechobe.channel.domain.ChannelVisibility;
import java.time.LocalDateTime;
import java.util.UUID;

// 채널 화면과 설정 화면이 공유하는 기본 정보와 현재 참여 상태를 전달한다.
public record ChannelDetailResponse(
        UUID id,
        String name,
        String description,
        ChannelVisibility visibility,
        @JsonProperty("isGeneral") boolean general,
        boolean joined,
        boolean createdByMe,
        ChannelStatus status,
        LocalDateTime archiveExpiresAt
) {

    public static ChannelDetailResponse from(Channel channel, boolean joined, UUID membershipId) {
        return new ChannelDetailResponse(
                channel.getId(),
                channel.getName(),
                channel.getDescription(),
                channel.getVisibility(),
                channel.isGeneral(),
                joined,
                channel.getCreatedByMembershipId().equals(membershipId),
                channel.getStatus(),
                channel.getArchiveExpiresAt()
        );
    }
}
