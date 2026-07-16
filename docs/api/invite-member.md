# Re-Echo 1차 MVP API 명세: 초대와 멤버

### 11.12 활성 초대 링크 조회

- Description: 현재 활성화된 초대 링크를 조회한다. 링크 복사처럼 기존
  초대 링크를 공유하는 동작은 이 API를 우선 사용해 활성 링크를
  재사용한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/invite-link`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.13 초대 링크 발급/명시적 재발급

- Description: 활성 초대 링크가 없으면 24시간 만료 초대 링크를
  발급한다. 사용자가 명시적으로 재발급을 요청한 경우에만 기존 활성
  링크를 무효화하고 새 링크로 교체한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/invite-link`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Success Response: `201 Created`

### 11.14 초대 링크 미리보기

- Description: 로그인 여부와 상관없이 초대 링크의 최소 워크스페이스 정보를 조회한다.
- Method: `GET`
- URL: `/api/v1/invite-links/{token}`
- Authentication: 불필요
- Authorization: 공개
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "workspaceName": "Re-Echo Team",
    "workspaceImageUrl": "https://...",
    "expiresAt": "2026-07-07T12:00:00"
  }
}
```

### 11.15 초대 링크 참여

- Description: 초대 링크로 워크스페이스에 참여한다. 계정 기본 프로필을
  멤버십 프로필 초기값으로 복사하고 `#general`에 자동 가입된다.
- Method: `POST`
- URL: `/api/v1/invite-links/{token}/join`
- Authentication: 필요
- Authorization: 공개 링크 + 로그인 사용자
- Error Responses
  - `401 AUTH_UNAUTHORIZED`
  - `404 INVITE_NOT_FOUND`
  - `409 INVITE_EXPIRED`
  - `409 MEMBER_REMOVED`
- Rules
  - 자진 탈퇴한 멤버가 다시 참여하면 기존 멤버십을 `ACTIVE`로 복구한다.
  - 강제 제거된 멤버는 초대 링크로 다시 참여할 수 없다.

### 11.16 워크스페이스 멤버 목록 조회

- Description: 워크스페이스 전체 멤버 목록을 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/members`
- Authentication: 필요
- Authorization: 해당 워크스페이스 멤버
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "contents": [
      {
        "id": "uuid",
        "displayName": "홍길동",
        "profileImageUrl": "https://...",
        "role": "MEMBER",
        "status": "ACTIVE"
      }
    ]
  }
}
```

- Note: 이메일은 노출하지 않는다.

### 11.17 멤버 역할 변경

- Description: 워크스페이스 멤버의 역할을 변경한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/members/{memberId}/role`
- Authentication: 필요
- Authorization: `OWNER`
- Request Body

```json
{
  "role": "ADMIN"
}
```

- `role`에는 `ADMIN` 또는 `MEMBER`만 지정할 수 있다. 소유자 권한 이전은
  MVP 범위에 포함하지 않는다.

- Error Responses
  - `409 MEMBER_LAST_OWNER_CHANGE_FORBIDDEN`

### 11.18 멤버 강제 제거

- Description: 멤버를 워크스페이스에서 강제 제거한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/members/{memberId}/remove`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.19 워크스페이스 자진 탈퇴

- Description: 본인이 워크스페이스를 탈퇴한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/members/{memberId}/leave`
- Authentication: 필요
- Authorization: 본인
- Error Responses
  - `409 MEMBER_LAST_OWNER_LEAVE_FORBIDDEN`
