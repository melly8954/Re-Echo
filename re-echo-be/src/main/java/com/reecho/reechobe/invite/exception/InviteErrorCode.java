package com.reecho.reechobe.invite.exception;

import com.reecho.reechobe.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 초대 도메인의 API 에러 코드를 관리한다.
public enum InviteErrorCode implements ErrorCode {
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다."),
    INVITE_EXPIRED(HttpStatus.CONFLICT, "초대 링크가 만료되었습니다."),
    INVITE_REVOKED(HttpStatus.CONFLICT, "초대 링크가 무효화되었습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    InviteErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
