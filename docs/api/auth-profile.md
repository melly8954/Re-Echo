# Re-Echo 1차 MVP API 명세: 인증과 계정 프로필

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
    "accessTokenExpiresAt": "2026-07-06T14:00:00Z"
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
    "expiresAt": "2026-07-06T12:10:00Z"
  }
}
```

- Constraints
  - 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`
  - 최대 크기: 10MB
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
