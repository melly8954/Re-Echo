package com.reecho.reechobe.common.response;

// validation 실패 필드와 사유를 클라이언트에 전달한다.
public record FieldErrorResponse(
        String field,
        String reason
) {
}
