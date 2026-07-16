import { apiRequest } from '../../shared/api/apiClient'
import type { AuthenticatedUser } from '../auth/authTypes'

export interface UpdateUserProfileRequest {
  displayName: string
  profileImageFileId?: string | null
}

interface ProfileImagePresignRequest {
  fileName: string
  contentType: string
  size: number
}

interface PresignedUploadResponse {
  fileId: string
  uploadUrl: string
  expiresAt: string
}

// 계정 기본 프로필과 R2 직접 업로드용 URL을 요청하는 사용자 API다.
export function updateUserProfile(request: UpdateUserProfileRequest) {
  return apiRequest<AuthenticatedUser>('/api/v1/users/me/profile', {
    method: 'PATCH',
    authenticated: true,
    body: JSON.stringify(request),
  })
}

export function createProfileImageUploadUrl(request: ProfileImagePresignRequest) {
  return apiRequest<PresignedUploadResponse>(
    '/api/v1/users/me/profile-image/presign-upload',
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export async function uploadProfileImageToStorage(uploadUrl: string, file: File) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  })

  if (!response.ok) {
    throw new Error('프로필 이미지 업로드에 실패했습니다.')
  }
}
