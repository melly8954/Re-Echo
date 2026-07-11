package com.reecho.reechobe.workspace.exception;

import com.reecho.reechobe.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 워크스페이스 도메인의 API 에러 코드를 관리한다.
public enum WorkspaceErrorCode implements ErrorCode {
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "워크스페이스 접근 권한이 없습니다."),
    WORKSPACE_NAME_CONFLICT(HttpStatus.CONFLICT, "이미 사용 중인 워크스페이스 이름입니다."),
    WORKSPACE_RESTORE_NOT_ALLOWED(HttpStatus.CONFLICT, "워크스페이스를 복원할 수 없습니다."),
    WORKSPACE_ARCHIVED(HttpStatus.CONFLICT, "보관된 워크스페이스입니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    WorkspaceErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
