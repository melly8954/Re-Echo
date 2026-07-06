package com.reecho.reechobe.common.response;

// C는 cursor 타입을 의미한다.
public record CursorPageInfo<C>(
        String type,
        int size,
        boolean hasNext,
        C nextCursor
) {

    public static <C> CursorPageInfo<C> of(int size, boolean hasNext, C nextCursor) {
        return new CursorPageInfo<>(
                "CURSOR",
                size,
                hasNext,
                nextCursor
        );
    }
}
