package com.reecho.reechobe.common.response;

import java.util.List;

// T는 목록 요소 타입, C는 cursor 타입을 의미한다.
public record CursorPageResponse<T, C>(
        List<T> contents,
        CursorPageInfo<C> page
) {
}
