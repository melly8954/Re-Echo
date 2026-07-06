package com.reecho.reechobe.common.response;

import java.util.List;

// validation 실패 목록을 공통 응답의 result에 담기 위한 구조다.
public record ValidationErrorResponse(
        List<FieldErrorResponse> fieldErrors
) {
}
