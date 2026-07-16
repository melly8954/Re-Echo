# Re-Echo Decision Records: 개요와 색인

## Decisions Overview

이 문서는 Re-Echo 1차 MVP 문서에 이미 확정된 주요 설계 결정을
ADR 형식으로 정리한다.

새로운 설계를 추가하지 않고, 기존 문서의 제품 범위, 아키텍처,
데이터 모델, API 계약, 코딩 컨벤션에서 확인되는 결정만 기록한다.

## Source Documents

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision Index

| Decision | Title | Status |
| --- | --- | --- |
| Decision 001 | 1차 MVP 범위를 채널 기반 SaaS 협업으로 제한 | Accepted |
| Decision 002 | Java Spring Boot 모놀리식 백엔드와 React Vite CSR SPA 선택 | Accepted |
| Decision 003 | PostgreSQL을 최종 진실 소스로 사용 | Accepted |
| Decision 004 | Redis를 보조 상태와 실시간 전파 계층으로 제한 | Accepted |
| Decision 005 | Access Token과 Refresh Token 저장 방식을 분리 | Accepted |
| Decision 006 | 워크스페이스 멤버십을 권한과 프로필의 기준으로 사용 | Accepted |
| Decision 007 | 채널은 기본 채널, 공개 채널, 비공개 채널 정책으로 운영 | Accepted |
| Decision 008 | 실시간 처리는 메시지와 입력 중 표시로 제한 | Accepted |
| Decision 009 | 메시지 목록은 cursor pagination으로 조회 | Accepted |
| Decision 010 | 읽음 상태는 채널별 마지막 읽은 메시지 기준으로 관리 | Accepted |
| Decision 011 | 파일은 R2 직접 전송과 DB 메타데이터로 분리 | Accepted |
| Decision 012 | 워크스페이스와 채널은 보관 후 만료 삭제 모델 사용 | Accepted |
| Decision 013 | REST API는 공통 응답 envelope과 에러 코드 체계를 사용 | Accepted |
| Decision 014 | DTO와 Entity를 분리하고 계층 책임을 명확히 유지 | Accepted |
| Decision 015 | Command Service와 Query Service를 책임 기준으로 분리 | Accepted |
| Decision 016 | 상태값은 문자열 enum 컬럼으로 시작 | Accepted |
| Decision 017 | Flyway 마이그레이션으로 DB 변경 이력을 관리 | Accepted |

## Decision Records
