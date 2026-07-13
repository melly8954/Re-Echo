package com.reecho.reechobe.member.dto;

import com.reecho.reechobe.member.domain.WorkspaceMembership;
import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import com.reecho.reechobe.member.domain.WorkspaceMembershipStatus;
import java.util.UUID;

// 워크스페이스 멤버 목록에 필요한 프로필과 권한 정보를 전달한다.
public record WorkspaceMemberResponse(
        UUID id,
        String displayName,
        String profileImageUrl,
        WorkspaceMembershipRole role,
        WorkspaceMembershipStatus status
) {

    public static WorkspaceMemberResponse from(WorkspaceMembership membership) {
        return new WorkspaceMemberResponse(
                membership.getId(),
                membership.getDisplayName(),
                membership.getProfileImageUrl(),
                membership.getRole(),
                membership.getStatus()
        );
    }
}
