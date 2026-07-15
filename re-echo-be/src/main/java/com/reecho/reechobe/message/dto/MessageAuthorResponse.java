package com.reecho.reechobe.message.dto;

import com.reecho.reechobe.member.domain.WorkspaceMembership;
import java.util.UUID;

// 메시지 행을 그리는 데 필요한 워크스페이스별 작성자 정보를 전달한다.
public record MessageAuthorResponse(
        UUID memberId,
        String displayName,
        String profileImageUrl
) {

    public static MessageAuthorResponse from(WorkspaceMembership membership) {
        return new MessageAuthorResponse(
                membership.getId(),
                membership.getDisplayName(),
                membership.getProfileImageUrl()
        );
    }
}
