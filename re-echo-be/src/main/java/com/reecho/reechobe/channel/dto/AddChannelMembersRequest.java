package com.reecho.reechobe.channel.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

// 비공개 채널에 추가할 워크스페이스 멤버십 식별자를 받는다.
public record AddChannelMembersRequest(
        @NotEmpty
        Set<@NotNull UUID> memberIds
) {
}
