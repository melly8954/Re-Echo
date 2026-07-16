# Re-Echo 1차 MVP API 명세: 채널 멤버


### 11.28 채널 멤버 목록 조회

- Description: 채널 멤버 목록을 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/members`
- Authentication: 필요
- Authorization
  - 공개 채널: 채널 접근 가능한 워크스페이스 멤버
  - 비공개 채널: 채널 멤버만 가능
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

### 11.29 비공개 채널 멤버 추가

- Description: 비공개 채널에 멤버를 추가한다. 추가된 멤버는 과거 메시지 전체를 조회할 수 있다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/members`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Request Body

```json
{
  "memberIds": [
    "uuid"
  ]
}
```

### 11.30 채널 멤버 강제 제거

- Description: 공개 또는 비공개 채널에서 멤버를 강제 제거한다. 강제
  제거된 멤버는 해당 채널에 재참여할 수 없다.
- Method: `DELETE`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/members/{memberId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Error Responses
  - `409 CHANNEL_MEMBER_REMOVED`
- Note
  - `#general`에서는 멤버를 제거할 수 없다.
  - 비공개 채널 생성자는 채널 보관 전까지 제거할 수 없다.
