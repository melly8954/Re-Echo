# Re-Echo 1차 MVP ERD: 관계와 상태값

## 8. Relationship Definitions

- `users` 1:N `user_identities`
- `users` 1:N `workspaces`
  - 생성자 기준 관계
- `users` 1:N `workspace_memberships`
- `users` 1:N `file_objects`
  - 업로더 사용자 기준 관계
- `file_objects` 1:N `users`
  - 계정 기본 프로필 이미지 참조 기준 관계
- `workspaces` 1:N `workspace_memberships`
- `workspaces` 1:N `workspace_invite_links`
- `workspaces` 1:N `channels`
- `workspace_memberships` 1:N `workspace_invite_links`
  - 발급자 기준 관계
- `workspace_memberships` 1:N `channel_memberships`
- `channels` 1:N `channel_memberships`
- `channels` 1:N `messages`
- `workspace_memberships` 1:N `messages`
  - 작성자 기준 관계
- `channel_memberships` 1:1 `channel_read_states`
- `messages` 1:N `message_attachments`
- `file_objects` 1:N `message_attachments`
- `workspaces` 1:N `file_objects`
- `workspace_memberships` 1:N `file_objects`
  - 업로더 기준 관계
- `file_objects` 1:N `workspace_memberships`
  - 워크스페이스별 프로필 이미지 참조 기준 관계

관계 원칙:

- N:M 관계는 모두 중간 엔티티로 분리한다.
- 채널 참여는 사용자 계정이 아니라 `workspace_membership`을 기준으로 연결한다.
- 메시지 작성, 초대 링크 발급, 메시지 첨부 파일 업로드는 모두
  워크스페이스 문맥을 가진 멤버십 FK로 추적한다.
- 계정 기본 프로필 이미지 업로드는 워크스페이스가 없을 수 있으므로
  사용자 FK로 추적한다.
- 계정 기본 표시 이름과 프로필 이미지는 `users`에 저장한다.
- 워크스페이스 생성·참여 시 계정 기본 프로필을 멤버십 프로필로 복사한다.
- 복사된 워크스페이스별 닉네임과 프로필 이미지는
  `workspace_memberships`에서 독립적으로 관리한다.

## 9. Status & Enum Definitions

### 9.1 User Status

- `ACTIVE`
- `DEACTIVATED`

### 9.2 Workspace Membership Role

- `OWNER`
- `ADMIN`
- `MEMBER`

### 9.3 Workspace Membership Status

- `ACTIVE`
- `LEFT`
- `REMOVED`

### 9.4 Invite Link Status

- `ACTIVE`
- `REVOKED`
- `EXPIRED`

### 9.5 Channel Visibility

- `PUBLIC`
- `PRIVATE`

### 9.6 Channel Status

- `ACTIVE`
- `ARCHIVED`
- `DELETED`

### 9.7 Channel Membership Status

- `ACTIVE`
- `LEFT`
- `REMOVED`

### 9.8 Message Status

- `ACTIVE`
- `DELETED`

### 9.9 File Status

- `ACTIVE`
- `ORPHANED`
- `DELETED`

### 9.10 File Purpose

- `MESSAGE_ATTACHMENT`
- `PROFILE_IMAGE`

## 10. Deletion / Archive / Expiration / History Policy
