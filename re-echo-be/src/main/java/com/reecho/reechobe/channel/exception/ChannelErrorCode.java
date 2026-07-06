package com.reecho.reechobe.channel.exception;

import com.reecho.reechobe.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 채널 도메인의 API 에러 코드를 관리한다.
public enum ChannelErrorCode implements ErrorCode {
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "채널을 찾을 수 없습니다."),
    CHANNEL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "채널 접근 권한이 없습니다."),
    CHANNEL_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 채널입니다."),
    CHANNEL_JOIN_FORBIDDEN(HttpStatus.FORBIDDEN, "채널에 참여할 수 없습니다."),
    CHANNEL_GENERAL_LEAVE_FORBIDDEN(HttpStatus.CONFLICT, "기본 채널에서는 나갈 수 없습니다."),
    CHANNEL_RESTORE_NOT_ALLOWED(HttpStatus.CONFLICT, "채널을 복원할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ChannelErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
