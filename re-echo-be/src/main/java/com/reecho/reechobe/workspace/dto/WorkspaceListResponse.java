package com.reecho.reechobe.workspace.dto;

import java.util.List;

// 워크스페이스 목록 API의 공통 목록 응답 형태를 유지한다.
public record WorkspaceListResponse(List<WorkspaceListItemResponse> contents) {

    public static WorkspaceListResponse of(List<WorkspaceListItemResponse> contents) {
        return new WorkspaceListResponse(contents);
    }
}
