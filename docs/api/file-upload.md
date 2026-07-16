# Re-Echo 1차 MVP API 명세: 파일 업로드와 다운로드

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
    "expiresAt": "2026-07-06T12:10:00Z"
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
- Response Body

```json
{
  "status": 200,
  "errorCode": null,
  "message": "OK",
  "result": {
    "downloadUrl": "https://r2-presigned-url",
    "expiresAt": "2026-07-06T12:10:00Z"
  }
}
```

- Error Responses
  - `403 FILE_ACCESS_DENIED`
  - `404 FILE_NOT_FOUND`
  - `400 FILE_UPLOAD_NOT_COMPLETED`
