package com.reecho.reechobe.member.dto;

import com.reecho.reechobe.member.domain.WorkspaceMembershipRole;
import jakarta.validation.constraints.NotNull;

// 소유자가 관리자 권한을 부여하거나 해제할 때 사용하는 요청 형식이다.
public record ChangeWorkspaceMemberRoleRequest(
        @NotNull
        WorkspaceMembershipRole role
) {
}
