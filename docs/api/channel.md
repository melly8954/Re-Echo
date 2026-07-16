# Re-Echo 1차 MVP API 명세: 채널

### 11.20 채널 목록 조회

- Description: 사용자가 접근 가능한 활성·보관 채널 목록을 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels`
- Authentication: 필요
- Authorization: 해당 워크스페이스 멤버
- Note: 공개 채널은 워크스페이스 멤버에게 표시되며, 비공개 채널은
  참여 중인 채널만 표시된다. 보관 채널은 읽기 전용으로 유지되고,
  `archiveExpiresAt`에 자동 삭제 예정 시각을 제공한다.
  `memberCount`는 활성 채널 멤버 수이며, 워크스페이스 홈의 채널 요약과
  사이드바가 같은 목록 응답을 재사용한다.
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
        "name": "general",
        "visibility": "PUBLIC",
        "isGeneral": true,
        "joined": true,
        "createdByMe": false,
        "status": "ACTIVE",
        "archiveExpiresAt": null,
        "unreadCount": 3,
        "memberCount": 12
      }
    ]
  }
}
```

### 11.21 채널 생성

- Description: 공개 또는 비공개 채널을 생성한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Request Body

```json
{
  "name": "design",
  "description": "디자인 논의",
  "visibility": "PRIVATE",
  "memberIds": [
    "uuid"
  ]
}
```

- Note: 비공개 채널 생성자는 자동 포함되고, 초기 멤버를 추가로 지정할 수 있다.
- Success Response: `201 Created`
- Response Body

```json
{
  "status": 201,
  "errorCode": null,
  "message": "채널이 생성되었습니다.",
  "result": {
    "id": "uuid"
  }
}
```

### 11.22 채널 상세 조회

- Description: 채널 기본 정보와 현재 사용자의 참여 상태를 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}`
- Authentication: 필요
- Authorization
  - 공개 채널: 워크스페이스 멤버
  - 비공개 채널: 채널 멤버만 가능
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "id": "uuid",
    "name": "design",
    "description": "디자인 논의",
    "visibility": "PRIVATE",
    "isGeneral": false,
    "joined": true,
    "createdByMe": false,
    "status": "ACTIVE",
    "archiveExpiresAt": null
  }
}
```

### 11.23 채널 정보 수정

- Description: 채널 이름, 설명을 수정한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Request Body

```json
{
  "name": "design",
  "description": "디자인 논의"
}
```

- Success Response: `200 OK`
- Error Responses
  - `409 CHANNEL_GENERAL_MANAGEMENT_FORBIDDEN`

### 11.24 채널 보관

- Description: 채널을 보관 상태로 전환한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/archive`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Success Response: `200 OK`
- Error Responses
  - `409 CHANNEL_ARCHIVED`
  - `409 CHANNEL_GENERAL_MANAGEMENT_FORBIDDEN`

### 11.25 채널 복원

- Description: 보관 후 15일 이내인 채널을 복원한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/restore`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Success Response: `200 OK`
- Error Responses
  - `409 CHANNEL_RESTORE_NOT_ALLOWED`
  - `409 CHANNEL_GENERAL_MANAGEMENT_FORBIDDEN`

### 11.26 공개 채널 참여

- Description: 공개 채널에 참여한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/join`
- Authentication: 필요
- Authorization: 워크스페이스 멤버
- Success Response: `200 OK`
- Error Responses
  - `409 CHANNEL_ALREADY_JOINED`
  - `409 CHANNEL_MEMBER_REMOVED`
  - `403 CHANNEL_JOIN_FORBIDDEN`

### 11.27 채널 나가기

- Description: 공개 또는 비공개 채널에서 자진 나간다. 비공개 채널에서
  나간 사용자는 목록에서 해당 채널이 사라지며, 재참여하려면 관리자가
  다시 추가해야 한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/leave`
- Authentication: 필요
- Authorization: 채널 멤버
- Success Response: `200 OK`
- Error Responses
  - `409 CHANNEL_GENERAL_LEAVE_FORBIDDEN`
  - `409 CHANNEL_CREATOR_LEAVE_FORBIDDEN`
