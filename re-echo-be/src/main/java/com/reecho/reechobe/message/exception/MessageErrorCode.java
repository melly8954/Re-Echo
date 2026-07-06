package com.reecho.reechobe.message.exception;

import com.reecho.reechobe.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 메시지 도메인의 API 에러 코드를 관리한다.
public enum MessageErrorCode implements ErrorCode {
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    MESSAGE_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "메시지를 수정할 권한이 없습니다."),
    MESSAGE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "메시지를 삭제할 권한이 없습니다."),
    MESSAGE_EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "메시지 내용 또는 첨부 파일이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    MessageErrorCode(HttpStatus httpStatus, String defaultMessage) {
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
