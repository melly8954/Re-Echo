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
- 동일 사용자의 다중 로그인 세션을 허용하며 logout은 현재 세션만 종료한다.
- 워크스페이스 참여 시 기본 채널 `#general`에 자동 참여한다.
- `#general`은 나갈 수 없다.
- 공개 채널은 워크스페이스 멤버라면 자유롭게 참여/나가기/재참여가 가능하다.
- 비공개 채널은 멤버가 아니면 목록에 표시되지 않고 메시지에 접근할 수 없다.
- 비공개 채널은 자진 나가기가 가능하며, 재참여는 관리자 추가를 통해서만 가능하다.
- 공개/비공개 채널 모두 강제 제거된 멤버는 재참여할 수 없다.
- 비공개 채널 생성자는 채널 보관 전까지 자진 나가기와 강제 제거가 불가능하다.
- 워크스페이스와 채널은 삭제 요청 시 즉시 물리 삭제하지 않고 보관 후 15일 뒤 자동 삭제한다.
- 메시지 목록은 cursor pagination을 사용하며 cursor 기준은 `createdAt + messageId` 조합이다.
- 읽음 갱신은 REST API로만 처리하고 읽음 상태 전용 실시간 브로드캐스트는 제공하지 않는다.
- 파일 Presigned URL 발급 API는 파일 1건씩 처리한다.
- 메시지 첨부 파일 최대 크기는 20MB다.
- 프로필 이미지는 이미지 파일만 허용하며 최대 크기는 10MB다.
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

- 시간 필드는 ISO-8601 `LocalDateTime` 문자열을 사용한다.
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

### 11.1 OAuth 로그인 시작

- Description: 백엔드가 OAuth Provider 인증 화면으로 리다이렉트한다.
- Method: `GET`
- URL: `/oauth2/authorization/{provider}`
- Authentication: 불필요
- Authorization: 공개
- Path Parameters
  - `provider`: `google`, `kakao`, `github`
- Response: OAuth Provider 로그인 화면으로 `302 Found` 리다이렉트

### 11.2 OAuth 로그인 콜백

- Description: OAuth Provider가 전달한 인가 결과를 Spring Security가
  처리하고, 로그인 또는 회원가입을 완료한다.
- Method: `GET`
- URL: `/login/oauth2/code/{provider}`
- Authentication: 불필요
- Authorization: 공개
- Path Parameters
  - `provider`: `google`, `kakao`, `github`
- 처리 방식
  - 백엔드가 인가 코드를 Access Token으로 교환한다.
  - 백엔드가 OAuth Provider 사용자 정보를 조회한다.
  - 백엔드가 내부 사용자 계정과 OAuth 식별자를 연결한다.
  - 백엔드가 Re-Echo Access Token과 Refresh Token을 발급한다.
  - Refresh Token은 `HttpOnly`, `Secure`, `SameSite=Lax` 쿠키로 설정한다.
  - Access Token은 리다이렉트 URL에 포함하지 않는다.
  - 로그인 성공 후 프론트엔드 OAuth 완료 URL로 리다이렉트한다.
  - 프론트엔드는 `/api/v1/auth/refresh`를 호출해 Access Token을
    응답 body로 받는다.
- 실패 처리
  - OAuth 상태값 오류, 사용자 취소, Provider 인증 실패, 내부 계정 연결
    실패는 로그인 화면으로 `302 Found` 리다이렉트한다.
  - 리다이렉트 Query Parameter
    - `errorCode`: `AUTH_OAUTH_AUTHENTICATION_FAILED`
  - 구체적인 실패 원인과 예외 메시지는 리다이렉트 URL에 포함하지 않는다.

### 11.3 Token Refresh

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
    "accessTokenExpiresAt": "2026-07-06T14:00:00"
  }
}
```

- Success Response: `200 OK`
- Error Responses
  - `401 AUTH_REFRESH_TOKEN_EXPIRED`
  - `401 AUTH_REFRESH_TOKEN_INVALID`

### 11.4 Logout

- Description: 유효한 Refresh Token이 있으면 현재 세션을 무효화하고,
  Refresh Token 쿠키를 만료한다.
- Method: `POST`
- URL: `/api/v1/auth/logout`
- Authentication: 선택
- Authorization: 불필요
- Request Body: 없음
- Idempotency
  - 세션이 이미 종료되었거나 Refresh Token 쿠키가 없거나 만료·무효
    상태여도 동일하게 성공 처리한다.
  - 모든 요청에서 Refresh Token 쿠키를 만료한다.
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "로그아웃되었습니다.",
  "result": null
}
```

- Success Response: `200 OK`

### 11.5 내 계정 조회

- Description: 현재 로그인한 사용자의 기본 계정 정보를 조회한다.
- Method: `GET`
- URL: `/api/v1/users/me`
- Authentication: 필요
- Authorization: 본인
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "id": "uuid",
    "displayName": "홍길동",
    "profileImageUrl": "https://...",
    "status": "ACTIVE"
  }
}
```

### 11.6 워크스페이스 목록 조회

- Description: 사용자가 속한 워크스페이스 목록을 사용자의 생성·참여 등록 순으로 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces`
- Authentication: 필요
- Authorization: 멤버십 보유 사용자
- Note: `lastVisitedAt`은 워크스페이스 마지막 진입 시각이며, 최초 참여 시에는
  참여 시각으로 초기화한다. 목록 정렬에는 사용하지 않고 로그인 후 복귀 대상을
  결정하는 데만 사용한다.
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
        "lastVisitedAt": "2026-07-06T12:00:00",
        "defaultChannelId": "uuid",
        "unreadChannelCount": 2
      }
    ]
  }
}
```

### 11.7 워크스페이스 생성

- Description: 새 워크스페이스를 생성하고 생성자를 `OWNER`로 등록한다.
  생성자의 계정 기본 프로필을 멤버십 프로필 초기값으로 복사하며 기본
  채널 `#general`을 함께 생성한다.
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
- Response Body

```json
{
  "status": 201,
  "errorCode": null,
  "message": "워크스페이스가 생성되었습니다.",
  "result": {
    "id": "uuid",
    "defaultChannelId": "uuid"
  }
}
```

- Error Responses
  - `400 VALIDATION_ERROR`
  - `409 WORKSPACE_NAME_CONFLICT`

### 11.8 워크스페이스 상세 조회

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
      "displayName": "홍길동",
      "profileImageUrl": "https://...",
      "role": "ADMIN",
      "status": "ACTIVE"
    },
    "defaultChannelId": "uuid",
    "canRestore": false
  }
}
```

### 11.9 워크스페이스 정보 수정

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

### 11.10 워크스페이스 보관

- Description: 워크스페이스를 보관 상태로 전환한다. 하위 채널도 함께 보관된다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/archive`
- Authentication: 필요
- Authorization: `OWNER`

### 11.11 워크스페이스 복원

- Description: 보관 후 15일 이내인 워크스페이스를 복원한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/restore`
- Authentication: 필요
- Authorization: `OWNER`
- Error Responses
  - `404 WORKSPACE_NOT_FOUND`
  - `409 WORKSPACE_RESTORE_NOT_ALLOWED`

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

### 11.20 채널 목록 조회

- Description: 사용자가 접근 가능한 채널 목록을 조회한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/channels`
- Authentication: 필요
- Authorization: 해당 워크스페이스 멤버
- Note: 공개 채널은 워크스페이스 멤버에게 표시되며, 비공개 채널은
  참여 중인 채널만 표시된다.
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

### 11.23 채널 정보 수정

- Description: 채널 이름, 설명을 수정한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.24 채널 보관

- Description: 채널을 보관 상태로 전환한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/archive`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

### 11.25 채널 복원

- Description: 보관 후 15일 이내인 채널을 복원한다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/restore`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`

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
        "deleted": false
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

### 11.36 업로드 Presigned URL 발급

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
    "expiresAt": "2026-07-06T12:10:00"
  }
}
```

- Error Responses
  - `400 FILE_SIZE_EXCEEDED`
  - `400 FILE_CONTENT_TYPE_NOT_ALLOWED`

### 11.37 다운로드 Presigned URL 발급

- Description: 파일 접근 권한을 검증한 뒤 다운로드 URL을 발급한다.
- Method: `GET`
- URL: `/api/v1/workspaces/{workspaceId}/files/{fileId}/download-url`
- Authentication: 필요
- Authorization: 파일이 연결된 워크스페이스/채널 접근 가능 사용자

### 11.38 계정 프로필 이미지 업로드 Presigned URL 발급

- Description: 계정 기본 프로필 이미지 1건에 대한 업로드 URL을 발급하고
  프로필 이미지 용도의 임시 파일 메타데이터를 생성한다.
- Method: `POST`
- URL: `/api/v1/users/me/profile-image/presign-upload`
- Authentication: 필요
- Authorization: 본인
- Request Body

```json
{
  "fileName": "profile.png",
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
    "expiresAt": "2026-07-06T12:10:00"
  }
}
```

- Constraints
  - 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`
  - 최대 크기: 10MB
- Error Responses
  - `400 FILE_SIZE_EXCEEDED`
  - `400 FILE_CONTENT_TYPE_NOT_ALLOWED`

### 11.39 워크스페이스 프로필 이미지 업로드 Presigned URL 발급

- Description: 워크스페이스 내 프로필 이미지 1건에 대한 업로드 URL을
  발급하고 프로필 이미지 용도의 임시 파일 메타데이터를 생성한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/members/me/profile-image/presign-upload`
- Authentication: 필요
- Authorization: 해당 워크스페이스 멤버
- Request Body: `11.38 계정 프로필 이미지 업로드 Presigned URL 발급`과 동일
- Response Body: `11.38 계정 프로필 이미지 업로드 Presigned URL 발급`과 동일
- Constraints
  - 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`
  - 최대 크기: 10MB
  - 서버는 파일 메타데이터에 `workspaceId`와 현재 사용자의
    `membershipId`를 함께 저장해 워크스페이스 프로필 이미지 문맥을
    고정한다.
- Error Responses
  - `400 FILE_SIZE_EXCEEDED`
  - `400 FILE_CONTENT_TYPE_NOT_ALLOWED`

### 11.40 계정 기본 프로필 수정

- Description: 워크스페이스가 없는 상태와 새 멤버십의 초기값으로 사용하는
  계정 기본 표시 이름과 프로필 이미지를 수정한다. 기존 멤버십 프로필은
  변경하지 않는다.
- Method: `PATCH`
- URL: `/api/v1/users/me/profile`
- Authentication: 필요
- Authorization: 본인
- Request Body

```json
{
  "displayName": "홍길동",
  "profileImageFileId": "uuid"
}
```

- Rules
  - `profileImageFileId`를 생략하면 기존 프로필 이미지를 유지한다.
  - `profileImageFileId`에 `null`을 전달하면 현재 프로필 이미지를 제거한다.
  - `profileImageFileId`에 UUID를 전달하면 서버가 본인 소유, 프로필 이미지
    용도, 업로드 완료 여부를 검증한 뒤 프로필 이미지로 연결한다.
- Error Responses
  - `400 FILE_UPLOAD_NOT_COMPLETED`
  - `403 FILE_ACCESS_DENIED`
  - `404 FILE_NOT_FOUND`
- Response Body: `11.5 내 계정 조회`의 `result`와 동일

### 11.41 워크스페이스 내 프로필 수정

- Description: 현재 워크스페이스에서 사용하는 표시 이름과 프로필
  이미지를 수정한다. 계정 기본 프로필과 다른 워크스페이스의 프로필은
  변경하지 않는다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}/members/me/profile`
- Authentication: 필요
- Authorization: 해당 워크스페이스 멤버
- Request Body

```json
{
  "displayName": "홍길동",
  "profileImageFileId": "uuid"
}
```

- Rules
  - `profileImageFileId`를 생략하면 기존 프로필 이미지를 유지한다.
  - `profileImageFileId`에 `null`을 전달하면 현재 워크스페이스 프로필
    이미지를 제거한다.
  - `profileImageFileId`에 UUID를 전달하면 서버가 본인 소유, 해당
    워크스페이스 문맥, 프로필 이미지 용도, 업로드 완료 여부를 검증한
    뒤 워크스페이스 프로필 이미지로 연결한다.
- Error Responses
  - `400 FILE_UPLOAD_NOT_COMPLETED`
  - `403 FILE_ACCESS_DENIED`
  - `404 FILE_NOT_FOUND`

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
- 정렬: 사용자의 생성·참여 등록 순

### 12.3 채널 목록

- Pagination: 생략 가능
- 정렬
  - `#general` 우선
  - 나머지는 최근 활동 순

## 13. File API

### 13.1 업로드 흐름

메시지 첨부 파일 업로드 흐름은 다음과 같다.

1. 클라이언트가 `/files/presign-upload`를 호출한다.
2. 서버가 `fileId`와 `uploadUrl`을 반환한다.
3. 클라이언트가 R2에 직접 업로드한다.
4. 클라이언트가 메시지 생성/수정 시 `fileIds`를 전달한다.
5. 서버가 파일을 최종 첨부 상태로 확정한다.

프로필 이미지 업로드 흐름은 다음과 같다.

1. 클라이언트가 프로필 이미지 Presign API를 호출한다.
2. 서버가 `fileId`와 `uploadUrl`을 반환한다.
3. 클라이언트가 R2에 직접 업로드한다.
4. 클라이언트가 프로필 수정 API에 `profileImageFileId`를 전달한다.
5. 서버가 파일을 검증한 뒤 프로필 이미지로 연결한다.

### 13.2 파일 정책

- 파일 메타데이터는 PostgreSQL에서 관리한다.
- 이미지 파일만 기본 미리보기를 지원한다.
- 메시지에 연결되지 않은 업로드 완료 파일은 임시 파일 상태로 남을 수 있다.
- 프로필에 연결되지 않은 프로필 이미지 업로드 파일도 임시 파일 상태로
  남을 수 있다.
- 프로필에서 제거되거나 교체된 이미지는 `ORPHANED` 상태로 표시한 뒤
  정리 스케줄러가 지연 삭제한다.
- orphan 파일은 배치로 정리한다.
- 파일 정리 스케줄러의 기본 실행 주기는 하루 1회 새벽 3시다.
- 프로필 이미지 정리 배치는 기본적으로 24시간 이상 참조되지 않은
  `PROFILE_IMAGE` 파일을 한 번에 최대 100개씩 처리한다.
- 정리 후보는 사용자 계정과 워크스페이스 멤버십의 프로필 이미지
  참조가 없어야 하며, 삭제 직전 참조 여부를 다시 확인한다.
- 실행 여부, 보관 유예 시간, 배치 크기, 실행 cron은 환경 설정으로
  조정할 수 있다.

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
  "occurredAt": "2026-07-06T12:00:00",
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
