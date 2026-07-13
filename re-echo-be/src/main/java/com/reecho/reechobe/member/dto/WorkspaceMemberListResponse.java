package com.reecho.reechobe.member.dto;

import java.util.List;

// 워크스페이스 멤버 목록 API의 공통 목록 응답 형태를 유지한다.
public record WorkspaceMemberListResponse(List<WorkspaceMemberResponse> contents) {

    public static WorkspaceMemberListResponse of(List<WorkspaceMemberResponse> contents) {
        return new WorkspaceMemberListResponse(contents);
    }
}
