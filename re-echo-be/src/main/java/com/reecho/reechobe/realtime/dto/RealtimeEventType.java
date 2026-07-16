package com.reecho.reechobe.realtime.dto;

// API 문서에 확정된 실시간 event 유형만 사용한다.
public enum RealtimeEventType {
    MESSAGE_CREATED,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    TYPING_UPDATED
}
