package com.reecho.reechobe.file.domain;

// 파일 메타데이터와 외부 스토리지 객체의 처리 상태를 표현한다.
public enum FileStatus {
    ACTIVE,
    ORPHANED,
    DELETED
}
