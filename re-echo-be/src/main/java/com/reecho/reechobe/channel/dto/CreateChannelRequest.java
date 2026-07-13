package com.reecho.reechobe.channel.dto;

import com.reecho.reechobe.channel.domain.ChannelVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

// 새 채널 생성에 필요한 기본 정보와 초기 참여 멤버를 받는다.
public record CreateChannelRequest(
        @NotBlank
        @Size(max = 80)
        String name,

        @Size(max = 300)
        String description,

        @NotNull
        ChannelVisibility visibility,

        Set<UUID> memberIds
) {
}
