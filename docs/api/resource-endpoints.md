# Re-Echo 1차 MVP API 명세: 리소스와 Endpoint 목록

## 9. Resource List

- Auth
- Users
- Workspaces
- Workspace Invite Links
- Workspace Members
- Channels
- Channel Members
- Messages
- Channel Read States
- Files
- Profiles

## 10. Endpoint List

| Resource | Method | URL | Description |
| --- | --- | --- | --- |
| Auth | GET | `/oauth2/authorization/{provider}` | OAuth 로그인 시작 |
| Auth | GET | `/login/oauth2/code/{provider}` | OAuth 로그인 콜백 |
| Auth | POST | `/auth/refresh` | Access/Refresh Token 재발급 |
| Auth | POST | `/auth/logout` | 로그아웃 |
| Users | GET | `/users/me` | 내 계정 조회 |
| Workspaces | GET | `/workspaces` | 내 워크스페이스 목록 조회 |
| Workspaces | POST | `/workspaces` | 워크스페이스 생성 |
| Workspaces | GET | `/workspaces/{workspaceId}` | 워크스페이스 상세 조회 |
| Workspaces | PATCH | `/workspaces/{workspaceId}` | 워크스페이스 정보 수정 |
| Workspaces | POST | `/workspaces/{workspaceId}/image/presign-upload` | 대표 이미지 업로드 Presigned URL 발급 |
| Workspaces | PATCH | `/workspaces/{workspaceId}/archive` | 워크스페이스 보관 |
| Workspaces | PATCH | `/workspaces/{workspaceId}/restore` | 워크스페이스 복원 |
| Invites | GET | `/workspaces/{workspaceId}/invite-link` | 활성 초대 링크 조회 |
| Invites | POST | `/workspaces/{workspaceId}/invite-link` | 초대 링크 발급/명시적 재발급 |
| Invites | GET | `/invite-links/{token}` | 초대 링크 미리보기 |
| Invites | POST | `/invite-links/{token}/join` | 초대 링크로 워크스페이스 참여 |
| Members | GET | `/workspaces/{workspaceId}/members` | 워크스페이스 멤버 목록 조회 |
| Members | PATCH | `/workspaces/{workspaceId}/members/{memberId}/role` | 멤버 역할 변경 |
| Members | POST | `/workspaces/{workspaceId}/members/{memberId}/remove` | 멤버 강제 제거 |
| Members | POST | `/workspaces/{workspaceId}/members/{memberId}/leave` | 워크스페이스 자진 탈퇴 |
| Channels | GET | `/workspaces/{workspaceId}/channels` | 채널 목록 조회 |
| Channels | POST | `/workspaces/{workspaceId}/channels` | 채널 생성 |
| Channels | GET | `/workspaces/{workspaceId}/channels/{channelId}` | 채널 상세 조회 |
| Channels | PATCH | `/workspaces/{workspaceId}/channels/{channelId}` | 채널 정보 수정 |
| Channels | PATCH | `/workspaces/{workspaceId}/channels/{channelId}/archive` | 채널 보관 |
| Channels | PATCH | `/workspaces/{workspaceId}/channels/{channelId}/restore` | 채널 복원 |
| Channel Members | POST | `/workspaces/{workspaceId}/channels/{channelId}/join` | 공개 채널 참여 |
| Channel Members | POST | `/workspaces/{workspaceId}/channels/{channelId}/leave` | 채널 나가기 |
| Channel Members | GET | `/workspaces/{workspaceId}/channels/{channelId}/members` | 채널 멤버 목록 조회 |
| Channel Members | POST | `/workspaces/{workspaceId}/channels/{channelId}/members` | 비공개 채널 멤버 추가 |
| Channel Members | DELETE | `/workspaces/{workspaceId}/channels/{channelId}/members/{memberId}` | 채널 멤버 강제 제거 |
| Messages | GET | `/workspaces/{workspaceId}/channels/{channelId}/messages` | 메시지 목록 조회 |
| Messages | POST | `/workspaces/{workspaceId}/channels/{channelId}/messages` | 메시지 생성 |
| Messages | PATCH | `/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}` | 본인 메시지 수정 |
| Messages | DELETE | `/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}` | 메시지 삭제 |
| Read States | PUT | `/workspaces/{workspaceId}/channels/{channelId}/read-state` | 채널 읽음 갱신 |
| Files | POST | `/workspaces/{workspaceId}/files/presign-upload` | 업로드 Presigned URL 발급 |
| Files | GET | `/workspaces/{workspaceId}/files/{fileId}/download-url` | 다운로드 Presigned URL 발급 |
| Profiles | POST | `/users/me/profile-image/presign-upload` | 계정 프로필 이미지 업로드 Presigned URL 발급 |
| Profiles | POST | `/workspaces/{workspaceId}/members/me/profile-image/presign-upload` | 워크스페이스 프로필 이미지 업로드 Presigned URL 발급 |
| Profiles | PATCH | `/users/me/profile` | 계정 기본 프로필 수정 |
| Profiles | PATCH | `/workspaces/{workspaceId}/members/me/profile` | 워크스페이스 내 프로필 수정 |

## 11. Endpoint Details
