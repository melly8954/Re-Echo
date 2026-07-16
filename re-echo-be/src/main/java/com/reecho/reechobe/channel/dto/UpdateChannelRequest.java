package com.reecho.reechobe.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 채널 관리자가 변경할 수 있는 이름과 설명을 받는다.
public record UpdateChannelRequest(
        @NotBlank
        @Size(max = 80)
        String name,

        @Size(max = 300)
        String description
) {
}
