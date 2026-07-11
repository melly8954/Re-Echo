package com.reecho.reechobe.common.exception;

import org.springframework.http.HttpStatus;

// 도메인별 에러 코드가 공통 응답 변환에 필요한 값을 제공한다.
public interface ErrorCode {

    HttpStatus getHttpStatus();

    String getDefaultMessage();

    default String getCode() {
        if (this instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return getClass().getSimpleName();
    }
}
