# Re-Echo 1차 MVP API 명세: 메시지

### 11.31 메시지 목록 조회

- Description: 채널 메시지 목록을 cursor pagination으로 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages`
- Authentication: 필요
- Authorization: 채널 접근 가능 사용자
- Query Parameters
  - `size`: 페이지 크기
  - `cursorCreatedAt`: 다음 페이지 기준 시각
  - `cursorMessageId`: 다음 페이지 기준 메시지 ID
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
        "channelId": "uuid",
        "author": {
          "memberId": "uuid",
          "displayName": "홍길동",
          "profileImageUrl": "https://..."
        },
        "content": "안녕하세요",
        "attachments": [
          {
            "fileId": "uuid",
            "fileName": "image.png",
            "contentType": "image/png",
            "size": 1200,
            "previewImage": true
          }
        ],
        "createdAt": "2026-07-06T12:00:00",
        "updatedAt": "2026-07-06T12:00:00",
        "edited": false,
        "deleted": false,
        "moderatorDeleted": false
      }
    ],
    "page": {
      "type": "CURSOR",
      "size": 30,
      "hasNext": true,
      "nextCursor": {
        "createdAt": "2026-07-06T11:59:00",
        "messageId": "uuid"
      }
    }
  }
}
```

### 11.32 메시지 생성

- Description: 텍스트와 첨부를 포함한 메시지를 생성한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages`
- Authentication: 필요
- Authorization: 채널 멤버
- Request Body

```json
{
  "content": "안녕하세요",
  "fileIds": [
    "uuid"
  ]
}
```

- Validation
  - `content`와 `fileIds`가 모두 비어 있으면 안 된다.
  - 보관된 채널에서는 생성할 수 없다.
- Success Response: `201 Created`, `result`에는 11.31의 메시지 항목과 같은
  전체 메시지 snapshot을 반환한다.

### 11.33 메시지 수정

- Description: 본인 메시지를 수정한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}`
- Authentication: 필요
- Authorization: 메시지 작성자 본인
- Request Body

```json
{
  "content": "수정된 내용",
  "fileIds": [
    "uuid"
  ]
}
```

- Note: 시간 제한 없이 수정 가능하다.
- Success Response: `200 OK`, `result`에는 11.31의 메시지 항목과 같은
  전체 메시지 snapshot을 반환한다.

### 11.34 메시지 삭제

- Description: 메시지를 소프트 삭제한다.
- Method: `DELETE`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}`
- Authentication: 필요
- Authorization
  - 본인 메시지 작성자
  - 또는 `OWNER`, `ADMIN`
- Note
  - 타인 메시지를 삭제한 `OWNER`, `ADMIN`의 삭제 결과는 메시지 snapshot의
    `moderatorDeleted: true`로 표시한다.
- Success Response: `200 OK`

### 11.35 채널 읽음 갱신

- Description: 채널의 마지막 읽은 메시지 기준점을 갱신한다.
- Method: `PUT`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/read-state`
- Authentication: 필요
- Authorization: 채널 멤버
- Request Body

```json
{
  "lastReadMessageId": "uuid"
}
```

- Note: 별도 read broadcast는 제공하지 않는다.
