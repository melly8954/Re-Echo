# Re-Echo 1차 MVP API 명세: 공통 요청과 응답 규칙

## 4. Common API Rules

### 4.1 Base URL

```text
/api/v1
```

### 4.2 Content Type

- Request: `application/json`
- Response: `application/json`
- 예외
  - Presigned URL 업로드: 스토리지 대상 content type 사용
  - WebSocket: STOMP frame 사용

### 4.3 시간과 ID

- 도메인 데이터의 시간 필드는 ISO-8601 `LocalDateTime` 문자열을 사용한다.
- Access Token·Presigned URL의 만료 시각과 WebSocket event의 `occurredAt`은
  ISO-8601 UTC `Instant` 문자열을 사용한다.
- 리소스 ID는 UUID 문자열을 사용한다.

### 4.4 공통 정렬 규칙

- 워크스페이스 목록: 사용자의 생성·참여 등록 순
- 채널 목록: `#general` 우선, 나머지는 최근 활동 순
- 메시지 목록: 최신 메시지 기준 진입, 과거 메시지는 역방향 페이징

## 5. Authentication & Authorization

### 5.1 인증 방식

- OAuth 로그인 완료 후 프론트엔드는 Token Refresh API를 호출해
  Access Token을 응답 body로 받는다.
- Refresh Token은 쿠키로만 전달한다.
- 보호된 REST API는 `Authorization: Bearer {accessToken}`을 사용한다.
- WebSocket handshake도 Access Token 기반 인증을 사용한다.

### 5.2 권한 정책

- `OWNER`
  - 관리자 권한 포함
  - 관리자 임명/해제 가능
  - 워크스페이스 이름 변경 가능
  - 워크스페이스 보관 가능
- `ADMIN`
  - 채널 생성 가능
  - 초대 링크 발급 가능
  - 워크스페이스 설명·대표 이미지 변경 가능
  - 멤버 강제 제거 가능
  - 비공개 채널 멤버 관리 가능
  - 모든 멤버 메시지 삭제 가능
- `MEMBER`
  - 메시지 작성 가능
  - 본인 메시지 수정 가능
  - 워크스페이스 자진 탈퇴 가능

## 6. Common Request Rules

### 6.1 Pagination Request

```json
{
  "page": {
    "type": "CURSOR",
    "size": 30,
    "cursor": {
      "createdAt": "2026-07-06T12:00:00",
      "messageId": "11111111-1111-1111-1111-111111111111"
    }
  }
}
```

- `cursor`는 opaque string이 아니라 객체 형태로 노출한다.
- 메시지 목록 외 일반 목록 API는 기본적으로 offset보다 단순 조회용 page/size 형태를 우선한다.

### 6.2 Validation Rules

- 문자열 필드는 앞뒤 공백 제거 후 검증한다.
- 빈 문자열 메시지는 허용하지 않는다.
- 첨부만 있는 메시지는 허용한다.
- 파일 업로드 Presign 요청은 `fileName`, `contentType`, `size`를 필수로 받는다.
- 프로필 이미지 Presign 요청은 `image/jpeg`, `image/png`, `image/webp`
  MIME 타입만 허용한다.

## 7. Common Response Format

모든 REST API는 다음 envelope을 사용한다.

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {}
}
```

### 7.1 목록 응답 예시

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "contents": [],
    "page": {
      "type": "CURSOR",
      "size": 30,
      "hasNext": true,
      "nextCursor": {
        "createdAt": "2026-07-06T12:00:00",
        "messageId": "11111111-1111-1111-1111-111111111111"
      }
    }
  }
}
```

- 목록 필드명은 `items`가 아니라 `contents`를 사용한다.

## 8. Common Error Response Format

```json
{
  "status": 400,
  "errorCode": "VALIDATION_ERROR",
  "message": "잘못된 요청입니다.",
  "result": {
    "fieldErrors": [
      {
        "field": "name",
        "reason": "must not be blank"
      }
    ]
  }
}
```
