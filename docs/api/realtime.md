# Re-Echo 1차 MVP API 명세: 실시간 이벤트

## 14. Real-time / Async API

### 14.1 WebSocket Endpoint

```text
/ws
```

- 클라이언트는 STOMP `CONNECT` frame의 `Authorization` header에
  `Bearer {accessToken}`을 전달한다.
- 서버는 `CONNECT`, `/sub/**` 구독, `/pub/**` 발행마다 인증 사용자와
  채널 멤버십을 검증한다.

### 14.2 구독 채널

```text
/sub/workspaces/{workspaceId}/channels/{channelId}
```

### 14.3 발행 채널

```text
/pub/workspaces/{workspaceId}/channels/{channelId}/messages
/pub/workspaces/{workspaceId}/channels/{channelId}/typing
```

- `messages` 발행 body는 11.32의 메시지 생성 요청과 같은
  `content`, `fileIds`를 사용한다.
- `typing` 발행 body는 아래와 같다.

```json
{
  "typing": true
}
```

- 입력 중 상태는 마지막 `typing: true` 발행 후 5초가 지나면 자동 해제한다.

### 14.4 Event Envelope

```json
{
  "eventId": "uuid",
  "type": "MESSAGE_CREATED",
  "occurredAt": "2026-07-06T12:00:00Z",
  "payload": {}
}
```

### 14.5 Message Event Payload

```json
{
  "message": {
    "id": "uuid",
    "channelId": "uuid",
    "author": {
      "memberId": "uuid",
      "displayName": "홍길동",
      "profileImageUrl": "https://..."
    },
    "content": "안녕하세요",
    "attachments": [],
    "createdAt": "2026-07-06T12:00:00",
    "updatedAt": "2026-07-06T12:00:00",
    "edited": false,
    "deleted": false
  }
}
```

- `MESSAGE_CREATED`
- `MESSAGE_UPDATED`
- `MESSAGE_DELETED`

모든 메시지 이벤트는 partial patch가 아니라 full snapshot을 전달한다.
클라이언트가 로컬 메시지 상태와 수신 이벤트의 최신성을 비교할 때는
event envelope의 `occurredAt`이 아니라 `payload.message.updatedAt`을
우선 기준으로 사용한다.

### 14.6 Typing Event Payload

```json
{
  "typingUserIds": [
    "uuid"
  ]
}
```

- `TYPING_UPDATED`
- snapshot 형태로 전달한다.

## 15. External Integration API

### 15.1 OAuth Provider

- 외부 OAuth Provider와 연동한다.
- 로그인 중간 상태는 Redis 공유 저장소를 사용한다.

### 15.2 Cloudflare R2

- 애플리케이션은 Presigned URL만 발급한다.
- 파일 바이너리 업로드/다운로드는 클라이언트와 R2가 직접 수행한다.
- 프로필 이미지 역시 애플리케이션 서버를 경유하지 않고 R2에 직접 업로드한다.

## 16. Error Code Definitions
