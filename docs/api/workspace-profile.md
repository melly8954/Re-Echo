# Re-Echo 1차 MVP API 명세: 워크스페이스 프로필

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
- Response Body: `11.16 워크스페이스 멤버 목록 조회`의 멤버 항목과 동일
