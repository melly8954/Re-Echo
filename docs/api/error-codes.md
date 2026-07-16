# Re-Echo 1차 MVP API 명세: 오류 코드와 설계 결정


### 16.1 Common

- `INTERNAL_SERVER_ERROR`
- `INVALID_REQUEST`
- `VALIDATION_ERROR`

### 16.2 Auth

- `AUTH_UNAUTHORIZED`
- `AUTH_OAUTH_AUTHENTICATION_FAILED`
- `AUTH_REFRESH_TOKEN_INVALID`
- `AUTH_REFRESH_TOKEN_EXPIRED`

### 16.3 Workspace

- `WORKSPACE_NOT_FOUND`
- `WORKSPACE_ACCESS_DENIED`
- `WORKSPACE_NAME_CONFLICT`
- `WORKSPACE_RESTORE_NOT_ALLOWED`
- `WORKSPACE_ARCHIVED`

### 16.4 Invite

- `INVITE_NOT_FOUND`
- `INVITE_EXPIRED`
- `INVITE_REVOKED`

### 16.5 Member

- `MEMBER_NOT_FOUND`
- `MEMBER_INVALID_DISPLAY_NAME`
- `MEMBER_REMOVED`
- `MEMBER_LAST_OWNER_CHANGE_FORBIDDEN`
- `MEMBER_LAST_OWNER_LEAVE_FORBIDDEN`
- `MEMBER_REMOVE_FORBIDDEN`

### 16.6 Channel

- `CHANNEL_NOT_FOUND`
- `CHANNEL_ACCESS_DENIED`
- `CHANNEL_ALREADY_JOINED`
- `CHANNEL_JOIN_FORBIDDEN`
- `CHANNEL_GENERAL_LEAVE_FORBIDDEN`
- `CHANNEL_GENERAL_MANAGEMENT_FORBIDDEN`
- `CHANNEL_MEMBER_REMOVED`
- `CHANNEL_CREATOR_LEAVE_FORBIDDEN`
- `CHANNEL_ARCHIVED`
- `CHANNEL_RESTORE_NOT_ALLOWED`

### 16.7 Message

- `MESSAGE_NOT_FOUND`
- `MESSAGE_EDIT_FORBIDDEN`
- `MESSAGE_DELETE_FORBIDDEN`
- `MESSAGE_EMPTY_CONTENT`

### 16.8 File

- `FILE_NOT_FOUND`
- `FILE_ACCESS_DENIED`
- `FILE_SIZE_EXCEEDED`
- `FILE_CONTENT_TYPE_NOT_ALLOWED`
- `FILE_UPLOAD_NOT_COMPLETED`

## 17. Design Decisions

- API Base Path는 `/api/v1`로 둔다.
- OAuth 로그인 시작과 콜백 처리는 Spring Security의 기본
  `/oauth2/authorization/{provider}`, `/login/oauth2/code/{provider}`
  패턴을 사용한다.
- 초대 링크는 워크스페이스당 활성 링크 1개 정책을 반영해 조회와 발급 리소스를 분리한다. 링크 복사/공유는 기존 활성 링크 조회를 우선하고, 명시적 재발급만 기존 링크를 무효화한다.
- 멤버 강제 제거와 자진 탈퇴는 단순 필드 수정이 아니라 권한/상태 검증이 큰 도메인 동작이므로 명령형 endpoint를 허용한다.
- 읽음 상태는 메시지별 영수증이 아니라 채널별 마지막 읽은 메시지 기준점으로 단순화한다.
- 파일 첨부는 업로드와 메시지 연결을 분리해 대용량 바이너리가 애플리케이션 서버를 통과하지 않게 한다.
- WebSocket은 메시지와 typing에 한정하고, 읽음 상태는 REST로만 처리한다.
- 보관 리소스는 복원 가능 여부를 상세 응답의 `canRestore`로 제공한다.
