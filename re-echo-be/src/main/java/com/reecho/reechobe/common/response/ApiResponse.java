package com.reecho.reechobe.common.response;

import org.springframework.http.HttpStatus;

// REST API의 공통 응답 envelope을 표현한다.
public record ApiResponse<T>(
        int status,
        String errorCode,
        String message,
        T result
) {

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T result) {
        return new ApiResponse<>(status.value(), null, message, result);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String errorCode, String message, T result) {
        return new ApiResponse<>(status.value(), errorCode, message, result);
    }
}
