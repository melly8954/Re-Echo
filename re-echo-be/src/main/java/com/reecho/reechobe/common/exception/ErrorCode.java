package com.reecho.reechobe.common.exception;

import org.springframework.http.HttpStatus;

// API 문서에 정의된 에러 코드와 기본 HTTP 상태를 관리한다.
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "OAuth 상태값이 올바르지 않습니다."),
    AUTH_OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 인증에 실패했습니다."),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh Token이 올바르지 않습니다."),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),

    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다."),
    WORKSPACE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "워크스페이스 접근 권한이 없습니다."),
    WORKSPACE_NAME_CONFLICT(HttpStatus.CONFLICT, "이미 사용 중인 워크스페이스 이름입니다."),
    WORKSPACE_RESTORE_NOT_ALLOWED(HttpStatus.CONFLICT, "워크스페이스를 복원할 수 없습니다."),
    WORKSPACE_ARCHIVED(HttpStatus.CONFLICT, "보관된 워크스페이스입니다."),

    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다."),
    INVITE_EXPIRED(HttpStatus.CONFLICT, "초대 링크가 만료되었습니다."),
    INVITE_REVOKED(HttpStatus.CONFLICT, "초대 링크가 무효화되었습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    MEMBER_BANNED(HttpStatus.CONFLICT, "재초대할 수 없는 멤버입니다."),
    MEMBER_LAST_OWNER_CHANGE_FORBIDDEN(HttpStatus.CONFLICT, "마지막 소유자의 역할은 변경할 수 없습니다."),
    MEMBER_LAST_OWNER_LEAVE_FORBIDDEN(HttpStatus.CONFLICT, "마지막 소유자는 탈퇴할 수 없습니다."),
    MEMBER_REMOVE_FORBIDDEN(HttpStatus.FORBIDDEN, "멤버를 제거할 권한이 없습니다."),

    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "채널을 찾을 수 없습니다."),
    CHANNEL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채널 접근 권한이 없습니다."),
    CHANNEL_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 채널입니다."),
    CHANNEL_JOIN_FORBIDDEN(HttpStatus.FORBIDDEN, "채널에 참여할 수 없습니다."),
    CHANNEL_GENERAL_LEAVE_FORBIDDEN(HttpStatus.CONFLICT, "기본 채널에서는 나갈 수 없습니다."),
    CHANNEL_RESTORE_NOT_ALLOWED(HttpStatus.CONFLICT, "채널을 복원할 수 없습니다."),

    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    MESSAGE_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "메시지를 수정할 권한이 없습니다."),
    MESSAGE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "메시지를 삭제할 권한이 없습니다."),
    MESSAGE_EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "메시지 내용 또는 첨부 파일이 필요합니다."),

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    FILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "파일 접근 권한이 없습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기 제한을 초과했습니다."),
    FILE_CONTENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
