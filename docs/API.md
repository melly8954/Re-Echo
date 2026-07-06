# Re-Echo 1차 MVP API 명세

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
- 워크스페이스 참여 시 기본 채널 `#general`에 자동 참여한다.
- `#general`은 나갈 수 없다.
- 공개 채널은 워크스페이스 멤버라면 자유롭게 참여/나가기가 가능하다.
- 비공개 채널은 멤버가 아니면 존재와 메시지에 접근할 수 없다.
- 워크스페이스와 채널은 삭제 요청 시 즉시 물리 삭제하지 않고 보관 후 15일 뒤 자동 삭제한다.
- 메시지 목록은 cursor pagination을 사용하며 cursor 기준은 `createdAt + messageId` 조합이다.
- 읽음 갱신은 REST API로만 처리하고 읽음 상태 전용 실시간 브로드캐스트는 제공하지 않는다.
- 파일 Presigned URL 발급 API는 파일 1건씩 처리한다.
- 파일 최대 크기는 20MB다.
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

- 시간 필드는 ISO-8601 UTC 문자열을 사용한다.
- 리소스 ID는 UUID 문자열을 사용한다.

### 4.4 공통 정렬 규칙

- 워크스페이스 목록: 최근 방문/활동 순
- 채널 목록: `#general` 우선, 나머지는 최근 활동 순
- 메시지 목록: 최신 메시지 기준 진입, 과거 메시지는 역방향 페이징

## 5. Authentication & Authorization

### 5.1 인증 방식

- OAuth 로그인 완료 후 Access Token을 응답 body로 반환한다.
- Refresh Token은 쿠키로만 전달한다.
- 보호된 REST API는 `Authorization: Bearer {accessToken}`을 사용한다.
- WebSocket handshake도 Access Token 기반 인증을 사용한다.

### 5.2 권한 정책

- `OWNER`
  - 관리자 권한 포함
  - 관리자 임명/해제 가능
  - 워크스페이스 보관 가능
- `ADMIN`
  - 채널 생성 가능
  - 초대 링크 발급 가능
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
      "createdAt": "2026-07-06T12:00:00Z",
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
        "createdAt": "2026-07-06T12:00:00Z",
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
| Auth | POST | `/auth/oauth/{provider}` | OAuth 로그인 완료 처리 |
| Auth | POST | `/auth/refresh` | Access/Refresh Token 재발급 |
| Auth | POST | `/auth/logout` | 로그아웃 |
| Users | GET | `/users/me` | 내 계정 조회 |
| Workspaces | GET | `/workspaces` | 내 워크스페이스 목록 조회 |
| Workspaces | POST | `/workspaces` | 워크스페이스 생성 |
| Workspaces | GET | `/workspaces/{workspaceId}` | 워크스페이스 상세 조회 |
| Workspaces | PATCH | `/workspaces/{workspaceId}` | 워크스페이스 정보 수정 |
| Workspaces | PATCH | `/workspaces/{workspaceId}/archive` | 워크스페이스 보관 |
| Workspaces | PATCH | `/workspaces/{workspaceId}/restore` | 워크스페이스 복원 |
| Invites | GET | `/workspaces/{workspaceId}/invite-link` | 활성 초대 링크 조회 |
| Invites | POST | `/workspaces/{workspaceId}/invite-link` | 초대 링크 발급/재발급 |
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
| Channel Members | POST | `/workspaces/{workspaceId}/channels/{channelId}/leave` | 공개 채널 나가기 |
| Channel Members | GET | `/workspaces/{workspaceId}/channels/{channelId}/members` | 채널 멤버 목록 조회 |
| Channel Members | POST | `/workspaces/{workspaceId}/channels/{channelId}/members` | 비공개 채널 멤버 추가 |
| Channel Members | DELETE | `/workspaces/{workspaceId}/channels/{channelId}/members/{memberId}` | 비공개 채널 멤버 제거 |
| Messages | GET | `/workspaces/{workspaceId}/channels/{channelId}/messages` | 메시지 목록 조회 |
| Messages | POST | `/workspaces/{workspaceId}/channels/{channelId}/messages` | 메시지 생성 |
| Messages | PATCH | `/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}` | 본인 메시지 수정 |
| Messages | DELETE | `/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}` | 메시지 삭제 |
| Read States | PUT | `/workspaces/{workspaceId}/channels/{channelId}/read-state` | 채널 읽음 갱신 |
| Files | POST | `/workspaces/{workspaceId}/files/presign-upload` | 업로드 Presigned URL 발급 |
| Files | GET | `/workspaces/{workspaceId}/files/{fileId}/download-url` | 다운로드 Presigned URL 발급 |
| Profiles | PATCH | `/users/me/profile` | 내 프로필 수정 |

## 11. Endpoint Details

### 11.1 OAuth 로그인 완료

- Description: OAuth 인가 결과를 받아 로그인 또는 회원가입을 완료한다.
- Method: `POST`
- URL: `/api/v1/auth/oauth/{provider}`
- Authentication: 불필요
- Authorization: 공개
- Path Parameters
  - `provider`: `google`, `kakao`, `github`
- Request Body

```json
{
  "authorizationCode": "string",
  "redirectUri": "https://example.com/oauth/callback"
}
```

- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "로그인에 성공했습니다.",
  "result": {
    "accessToken": "jwt",
    "accessTokenExpiresAt": "2026-07-06T13:00:00Z",
    "user": {
      "id": "uuid",
      "displayName": "홍길동",
      "profileImageUrl": "https://..."
    }
  }
}
```

- Success Response: `200 OK`
- Error Responses
  - `400 INVALID_REQUEST`
  - `401 AUTH_INVALID_OAUTH_STATE`
  - `401 AUTH_OAUTH_AUTHENTICATION_FAILED`

### 11.2 Token Refresh

- Description: Access Token과 Refresh Token을 모두 재발급한다.
- Method: `POST`
- URL: `/api/v1/auth/refresh`
- Authentication: Refresh Token 쿠키 필요
- Authorization: 인증 사용자
- Request Body: 없음
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "토큰이 재발급되었습니다.",
  "result": {
    "accessToken": "jwt",
    "accessTokenExpiresAt": "2026-07-06T14:00:00Z"
  }
}
```

- Success Response: `200 OK`
- Error Responses
  - `401 AUTH_REFRESH_TOKEN_EXPIRED`
  - `401 AUTH_REFRESH_TOKEN_INVALID`

### 11.3 Logout

- Description: Refresh Token을 무효화하고 세션을 종료한다.
- Method: `POST`
- URL: `/api/v1/auth/logout`
- Authentication: Access Token 또는 Refresh Token 쿠키
- Authorization: 인증 사용자
- Request Body: 없음
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "로그아웃되었습니다.",
  "result": null
}
```

### 11.4 내 계정 조회

- Description: 현재 로그인한 사용자의 기본 계정 정보를 조회한다.
- Method: `GET`
- URL: `/api/v1/users/me`
- Authentication: 필요
- Authorization: 본인

### 11.5 워크스페이스 목록 조회

- Description: 사용자가 속한 워크스페이스 목록을 최근 방문/활동 순으로 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces`
- Authentication: 필요
- Authorization: 멤버십 보유 사용자
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
        "name": "Re-Echo Team",
        "imageUrl": "https://...",
        "role": "OWNER",
        "status": "ACTIVE",
        "lastVisitedAt": "2026-07-06T12:00:00Z",
        "defaultChannelId": "uuid",
        "unreadChannelCount": 2
      }
    ]
  }
}
```

### 11.6 워크스페이스 생성

- Description: 새 워크스페이스를 생성하고 생성자를 `OWNER`로 등록한다. 기본 채널 `#general`을 함께 생성한다.
- Method: `POST`
- URL: `/api/v1/workspaces`
- Authentication: 필요
- Authorization: 인증 사용자
- Request Body

```json
{
  "name": "Re-Echo Team",
  "description": "팀 워크스페이스",
  "imageUrl": "https://..."
}
```

- Success Response: `201 Created`
- Error Responses
  - `400 VALIDATION_ERROR`
  - `409 WORKSPACE_NAME_CONFLICT`

### 11.7 워크스페이스 상세 조회

- Description: 워크스페이스 기본 정보와 현재 사용자의 멤버십 정보를 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}`
- Authentication: 필요
- Authorization: 해당 워크스페이스 멤버
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "id": "uuid",
    "name": "Re-Echo Team",
    "description": "팀 워크스페이스",
    "imageUrl": "https://...",
    "status": "ACTIVE",
    "myMembership": {
      "id": "uuid",
      "role": "ADMIN",
      "status": "ACTIVE"
    },
    "defaultChannelId": "uuid",
    "canRestore": false
  }
}
```

### 11.8 워크스페이스 정보 수정

- Description: 워크스페이스 이름, 설명, 이미지를 수정한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Request Body

```json
{
  "name": "Re-Echo Team",
  "description": "새 설명",
  "imageUrl": "https://..."
}
```

### 11.9 워크스페이스 보관

- Description: 워크스페이스를 보관 상태로 전환한다. 하위 채널도 함께 보관된다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/archive`
- Authentication: 필요
- Authorization: `OWNER`

### 11.10 워크스페이스 복원

- Description: 보관 후 15일 이내인 워크스페이스를 복원한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/restore`
- Authentication: 필요
- Authorization: `OWNER`
- Error Responses
  - `404 WORKSPACE_NOT_FOUND`
  - `409 WORKSPACE_RESTORE_NOT_ALLOWED`

### 11.11 활성 초대 링크 조회

- Description: 현재 활성화된 초대 링크를 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/invite-link`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.12 초대 링크 발급/재발급

- Description: 24시간 만료 초대 링크를 발급한다. 기존 활성 링크가 있으면 무효화하고 새 링크로 교체한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/invite-link`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Success Response: `201 Created`

### 11.13 초대 링크 미리보기

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
    "expiresAt": "2026-07-07T12:00:00Z"
  }
}
```

### 11.14 초대 링크 참여

- Description: 초대 링크로 워크스페이스에 참여한다. 참여 후 `#general`에 자동 가입된다.
- Method: `POST`
- URL: `/api/v1/invite-links/{token}/join`
- Authentication: 필요
- Authorization: 공개 링크 + 로그인 사용자
- Error Responses
  - `401 AUTH_UNAUTHORIZED`
  - `404 INVITE_NOT_FOUND`
  - `409 INVITE_EXPIRED`
  - `409 MEMBER_BANNED`

### 11.15 워크스페이스 멤버 목록 조회

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

### 11.16 멤버 역할 변경

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

- Error Responses
  - `409 MEMBER_LAST_OWNER_CHANGE_FORBIDDEN`

### 11.17 멤버 강제 제거

- Description: 멤버를 워크스페이스에서 강제 제거한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/members/{memberId}/remove`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.18 워크스페이스 자진 탈퇴

- Description: 본인이 워크스페이스를 탈퇴한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/members/{memberId}/leave`
- Authentication: 필요
- Authorization: 본인
- Error Responses
  - `409 MEMBER_LAST_OWNER_LEAVE_FORBIDDEN`

### 11.19 채널 목록 조회

- Description: 사용자가 접근 가능한 채널 목록을 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels`
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
        "name": "general",
        "visibility": "PUBLIC",
        "isGeneral": true,
        "joined": true,
        "unreadCount": 3
      }
    ]
  }
}
```

### 11.20 채널 생성

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

### 11.21 채널 상세 조회

- Description: 채널 기본 정보와 현재 사용자의 참여 상태를 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}`
- Authentication: 필요
- Authorization
  - 공개 채널: 워크스페이스 멤버
  - 비공개 채널: 채널 멤버만 가능

### 11.22 채널 정보 수정

- Description: 채널 이름, 설명을 수정한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.23 채널 보관

- Description: 채널을 보관 상태로 전환한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/archive`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.24 채널 복원

- Description: 보관 후 15일 이내인 채널을 복원한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/restore`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.25 공개 채널 참여

- Description: 공개 채널에 참여한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/join`
- Authentication: 필요
- Authorization: 워크스페이스 멤버
- Error Responses
  - `409 CHANNEL_ALREADY_JOINED`
  - `403 CHANNEL_JOIN_FORBIDDEN`

### 11.26 공개 채널 나가기

- Description: 공개 채널에서 나간다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/leave`
- Authentication: 필요
- Authorization: 채널 멤버
- Error Responses
  - `409 CHANNEL_GENERAL_LEAVE_FORBIDDEN`

### 11.27 채널 멤버 목록 조회

- Description: 채널 멤버 목록을 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/members`
- Authentication: 필요
- Authorization
  - 공개 채널: 채널 접근 가능한 워크스페이스 멤버
  - 비공개 채널: 채널 멤버만 가능

### 11.28 비공개 채널 멤버 추가

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

### 11.29 비공개 채널 멤버 제거

- Description: 비공개 채널에서 멤버를 제거한다.
- Method: `DELETE`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/members/{memberId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.30 메시지 목록 조회

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
        "createdAt": "2026-07-06T12:00:00Z",
        "updatedAt": "2026-07-06T12:00:00Z",
        "edited": false,
        "deleted": false
      }
    ],
    "page": {
      "type": "CURSOR",
      "size": 30,
      "hasNext": true,
      "nextCursor": {
        "createdAt": "2026-07-06T11:59:00Z",
        "messageId": "uuid"
      }
    }
  }
}
```

### 11.31 메시지 생성

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

### 11.32 메시지 수정

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

### 11.33 메시지 삭제

- Description: 메시지를 소프트 삭제한다.
- Method: `DELETE`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages/{messageId}`
- Authentication: 필요
- Authorization
  - 본인 메시지 작성자
  - 또는 `OWNER`, `ADMIN`
- Success Response: `200 OK`

### 11.34 채널 읽음 갱신

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

### 11.35 업로드 Presigned URL 발급

- Description: 파일 1건에 대한 업로드 URL을 발급하고 임시 파일 메타데이터를 생성한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/files/presign-upload`
- Authentication: 필요
- Authorization: 워크스페이스 멤버
- Request Body

```json
{
  "fileName": "image.png",
  "contentType": "image/png",
  "size": 1200
}
```

- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "fileId": "uuid",
    "uploadUrl": "https://r2-presigned-url",
    "expiresAt": "2026-07-06T12:10:00Z"
  }
}
```

- Error Responses
  - `400 FILE_SIZE_EXCEEDED`
  - `400 FILE_CONTENT_TYPE_NOT_ALLOWED`

### 11.36 다운로드 Presigned URL 발급

- Description: 파일 접근 권한을 검증한 뒤 다운로드 URL을 발급한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/files/{fileId}/download-url`
- Authentication: 필요
- Authorization: 파일이 연결된 워크스페이스/채널 접근 가능 사용자

### 11.37 내 프로필 수정

- Description: 사용자 표시 이름과 프로필 이미지를 수정한다.
- Method: `PATCH`
- URL: `/api/v1/users/me/profile`
- Authentication: 필요
- Authorization: 본인
- Request Body

```json
{
  "displayName": "홍길동",
  "profileImageUrl": "https://..."
}
```

## 12. Pagination / Sorting / Filtering

### 12.1 메시지 목록

- Pagination: Cursor
- 기본 크기: `30`
- 최대 크기: `100`
- Cursor 기준: `createdAt`, `messageId`
- 정렬: `createdAt desc`, `messageId desc`
- `nextCursor`: 더 과거 메시지를 조회하기 위한 cursor

### 12.2 워크스페이스 목록

- Pagination: 생략 가능
- 정렬: 최근 방문/활동 순

### 12.3 채널 목록

- Pagination: 생략 가능
- 정렬
  - `#general` 우선
  - 나머지는 최근 활동 순

## 13. File API

### 13.1 업로드 흐름

1. 클라이언트가 `/files/presign-upload`를 호출한다.
2. 서버가 `fileId`와 `uploadUrl`을 반환한다.
3. 클라이언트가 R2에 직접 업로드한다.
4. 클라이언트가 메시지 생성/수정 시 `fileIds`를 전달한다.
5. 서버가 파일을 최종 첨부 상태로 확정한다.

### 13.2 파일 정책

- 파일 메타데이터는 PostgreSQL에서 관리한다.
- 이미지 파일만 기본 미리보기를 지원한다.
- 메시지에 연결되지 않은 업로드 완료 파일은 임시 파일 상태로 남을 수 있다.
- orphan 파일은 배치로 정리한다.

## 14. Real-time / Async API

### 14.1 WebSocket Endpoint

```text
/ws
```

### 14.2 구독 채널

```text
/sub/workspaces/{workspaceId}/channels/{channelId}
```

### 14.3 발행 채널

```text
/pub/workspaces/{workspaceId}/channels/{channelId}/messages
/pub/workspaces/{workspaceId}/channels/{channelId}/typing
```

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
    "content": "안녕하세요",
    "attachments": [],
    "createdAt": "2026-07-06T12:00:00Z",
    "updatedAt": "2026-07-06T12:00:00Z"
  }
}
```

- `MESSAGE_CREATED`
- `MESSAGE_UPDATED`
- `MESSAGE_DELETED`

모든 메시지 이벤트는 partial patch가 아니라 full snapshot을 전달한다.

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

## 16. Error Code Definitions

### 16.1 Common

- `INTERNAL_SERVER_ERROR`
- `INVALID_REQUEST`
- `VALIDATION_ERROR`

### 16.2 Auth

- `AUTH_UNAUTHORIZED`
- `AUTH_INVALID_OAUTH_STATE`
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
- `MEMBER_BANNED`
- `MEMBER_LAST_OWNER_CHANGE_FORBIDDEN`
- `MEMBER_LAST_OWNER_LEAVE_FORBIDDEN`
- `MEMBER_REMOVE_FORBIDDEN`

### 16.6 Channel

- `CHANNEL_NOT_FOUND`
- `CHANNEL_ACCESS_DENIED`
- `CHANNEL_ALREADY_JOINED`
- `CHANNEL_JOIN_FORBIDDEN`
- `CHANNEL_GENERAL_LEAVE_FORBIDDEN`
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

## 17. Design Decisions

- API Base Path는 `/api/v1`로 둔다.
- 로그인 완료 API는 provider별 분기보다 `/auth/oauth/{provider}` 단일 패턴으로 통일한다.
- 초대 링크는 워크스페이스당 활성 링크 1개 정책을 반영해 조회와 발급 리소스를 분리한다.
- 멤버 강제 제거와 자진 탈퇴는 단순 필드 수정이 아니라 권한/상태 검증이 큰 도메인 동작이므로 명령형 endpoint를 허용한다.
- 읽음 상태는 메시지별 영수증이 아니라 채널별 마지막 읽은 메시지 기준점으로 단순화한다.
- 파일 첨부는 업로드와 메시지 연결을 분리해 대용량 바이너리가 애플리케이션 서버를 통과하지 않게 한다.
- WebSocket은 메시지와 typing에 한정하고, 읽음 상태는 REST로만 처리한다.
- 보관 리소스는 복원 가능 여부를 상세 응답의 `canRestore`로 제공한다.
