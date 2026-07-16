# Re-Echo 1차 MVP API 명세: 워크스페이스

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
  "description": "팀 워크스페이스"
}
```

- 대표 이미지는 생성 후 `11.9.1`의 Presigned URL 업로드와 워크스페이스
  정보 수정 흐름으로 설정한다.

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
    "imageFileId": "uuid",
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

- Description: 소유자는 워크스페이스 이름, 설명, 이미지를 수정한다.
  관리자는 현재 이름을 유지한 채 설명과 이미지만 수정할 수 있다.
- Method: `PATCH`
- URL: `/api/v1/workspaces/{workspaceId}`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Request Body

```json
{
  "name": "Re-Echo Team",
  "description": "새 설명",
  "imageFileId": "uuid"
}
```

- `imageFileId`에 `null`을 전달하면 대표 이미지를 제거한다.
- `ADMIN`이 현재 이름과 다른 `name`을 전달하면 `403 WORKSPACE_ACCESS_DENIED`를
  반환한다.
- Response Body: `11.8 워크스페이스 상세 조회`와 동일

### 11.9.1 워크스페이스 대표 이미지 업로드 Presigned URL 발급

- Description: 워크스페이스 대표 이미지 1건에 대한 업로드 URL을 발급하고
  대표 이미지 용도의 임시 파일 메타데이터를 생성한다.
- Method: `POST`
- URL: `/api/v1/workspaces/{workspaceId}/image/presign-upload`
- Authentication: 필요
- Authorization: `OWNER`, `ADMIN`
- Request Body: `11.38 계정 프로필 이미지 업로드 Presigned URL 발급`과 동일
- Response Body: `11.38 계정 프로필 이미지 업로드 Presigned URL 발급`과 동일
- Constraints
  - 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`
  - 최대 크기: 10MB
  - 서버는 파일 메타데이터에 `workspaceId`와 현재 사용자의 `membershipId`를
    함께 저장해 워크스페이스 대표 이미지 문맥을 고정한다.

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
