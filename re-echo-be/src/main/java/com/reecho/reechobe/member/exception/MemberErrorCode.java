package com.reecho.reechobe.member.exception;

import com.reecho.reechobe.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 멤버 도메인의 API 에러 코드를 관리한다.
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    MEMBER_INVALID_DISPLAY_NAME(HttpStatus.BAD_REQUEST, "표시 이름은 1자 이상 80자 이하여야 합니다."),
    MEMBER_BANNED(HttpStatus.CONFLICT, "재초대할 수 없는 멤버입니다."),
    MEMBER_LAST_OWNER_CHANGE_FORBIDDEN(HttpStatus.CONFLICT, "마지막 소유자의 역할은 변경할 수 없습니다."),
    MEMBER_LAST_OWNER_LEAVE_FORBIDDEN(HttpStatus.CONFLICT, "마지막 소유자는 탈퇴할 수 없습니다."),
    MEMBER_REMOVE_FORBIDDEN(HttpStatus.FORBIDDEN, "멤버를 제거할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    MemberErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
