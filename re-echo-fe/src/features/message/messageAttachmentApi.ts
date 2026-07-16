import { apiRequest } from '../../shared/api/apiClient'

interface MessageAttachmentPresignRequest {
  fileName: string
  contentType: string
  size: number
}

interface PresignedUploadResponse {
  fileId: string
  uploadUrl: string
  expiresAt: string
}

interface PresignedDownloadResponse {
  downloadUrl: string
  expiresAt: string
}

// 메시지 첨부는 API 서버를 거치지 않고 발급받은 URL로 R2에 직접 업로드한다.
export function createMessageAttachmentUploadUrl(
  workspaceId: string,
  request: MessageAttachmentPresignRequest,
) {
  return apiRequest<PresignedUploadResponse>(
    `/api/v1/workspaces/${workspaceId}/files/presign-upload`,
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export function createMessageAttachmentDownloadUrl(workspaceId: string, fileId: string) {
  return apiRequest<PresignedDownloadResponse>(
    `/api/v1/workspaces/${workspaceId}/files/${fileId}/download-url`,
    {
      method: 'GET',
      authenticated: true,
    },
  )
}

export function uploadMessageAttachmentToStorage(
  uploadUrl: string,
  file: File,
  onProgress: (progress: number) => void,
) {
  return new Promise<void>((resolve, reject) => {
    const request = new XMLHttpRequest()
    request.open('PUT', uploadUrl)
    request.setRequestHeader('Content-Type', file.type || 'application/octet-stream')
    request.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    })
    request.addEventListener('load', () => {
      if (request.status >= 200 && request.status < 300) {
        onProgress(100)
        resolve()
        return
      }
      reject(new Error('첨부 파일 업로드에 실패했습니다.'))
    })
    request.addEventListener('error', () => reject(new Error('첨부 파일 업로드에 실패했습니다.')))
    request.send(file)
  })
}
