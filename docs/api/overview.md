# Re-Echo 1차 MVP API 명세: 개요와 설계 원칙

## 1. API Overview

- 목적: Re-Echo 1차 MVP의 클라이언트-서버 계약을 정의한다.
- 범위: 인증, 워크스페이스, 초대, 멤버, 채널, 메시지, 파일, 읽음 처리, 프로필, 실시간 이벤트
- 제외: DM, 메시지 검색, 멘션, AI 기능, 음성/화상 협업, 푸시 알림
- 통신 방식
  - 동기 요청/응답: REST API
  - 실시간 메시지/입력 중 표시: WebSocket
  - 파일 업로드/다운로드: Presigned URL 기반 직접 전송

## 2. Confirmed API Requirements

### 2.1 기능 범위

- 소셜 로그인: `Google`, `Kakao`, `GitHub`
- 워크스페이스 생성, 목록 조회, 상세 조회, 보관, 복원
- 초대 링크 발급, 조회, 재발급, 참여
- 역할 기반 멤버 관리: `OWNER`, `ADMIN`, `MEMBER`
- 공개/비공개 채널 운영
- 실시간 메시지 송수신과 입력 중 표시
- 메시지 수정, 삭제
- 파일 첨부와 이미지 미리보기
- 채널 단위 읽음 처리와 안읽음 배지

### 2.2 확정 정책

- Access Token은 응답 body로 전달한다.
- Refresh Token은 `HttpOnly`, `Secure`, `SameSite=Lax` 쿠키로만 전달한다.
- Refresh 시 Access Token과 Refresh Token을 모두 재발급한다.
- 동일 사용자의 다중 로그인 세션을 허용하며 logout은 현재 세션만 종료한다.
- 워크스페이스 참여 시 기본 채널 `#general`에 자동 참여한다.
- `#general`은 나갈 수 없다.
- 활성 워크스페이스의 `#general`은 이름·설명 수정, 개별 보관·복원을
  허용하지 않는다. 워크스페이스 보관·복원 시에만 함께 상태가 전환된다.
- 공개 채널은 워크스페이스 멤버라면 자유롭게 참여/나가기/재참여가 가능하다.
- 비공개 채널은 멤버가 아니면 목록에 표시되지 않고 메시지에 접근할 수 없다.
- 비공개 채널은 자진 나가기가 가능하며, 재참여는 관리자 추가를 통해서만 가능하다.
- 공개/비공개 채널 모두 강제 제거된 멤버는 재참여할 수 없다.
- 비공개 채널 생성자는 채널 보관 전까지 자진 나가기와 강제 제거가 불가능하다.
- 워크스페이스와 채널은 삭제 요청 시 즉시 물리 삭제하지 않고 보관 후 15일 뒤 자동 삭제한다.
- 메시지 목록은 cursor pagination을 사용하며 cursor 기준은 `createdAt + messageId` 조합이다.
- 읽음 갱신은 REST API로만 처리하고 읽음 상태 전용 실시간 브로드캐스트는 제공하지 않는다.
- 파일 Presigned URL 발급 API는 파일 1건씩 처리한다.
- 메시지 첨부 파일 최대 크기는 20MB다.
- 프로필 이미지는 이미지 파일만 허용하며 최대 크기는 10MB다.
- 공통 에러 코드는 prefix 없이 `INTERNAL_SERVER_ERROR`, `INVALID_REQUEST`, `VALIDATION_ERROR`를 사용한다.
- 도메인 에러 코드는 `AUTH_*`, `WORKSPACE_*`, `CHANNEL_*`, `MESSAGE_*`, `FILE_*`, `INVITE_*`, `MEMBER_*` prefix를 사용한다.
- WebSocket 이벤트 타입은 `MESSAGE_CREATED`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`, `TYPING_UPDATED`만 사용한다.

## 3. API Design Principles

- REST API는 리소스 중심으로 설계한다.
- 상태 변경은 기본적으로 `PATCH`로 표현한다.
- 문서에 없는 기능은 Endpoint로 추가하지 않는다.
- DB 엔티티 전체를 그대로 노출하지 않고 화면 동작에 필요한 DTO만 노출한다.
- 권한은 URL이 아니라 인증/인가 규칙으로 명시한다.
- 실시간 이벤트 payload는 부분 변경이 아니라 화면 갱신에 바로 사용할 수 있는 snapshot으로 전달한다.
