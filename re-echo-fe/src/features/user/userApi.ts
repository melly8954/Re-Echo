import { apiRequest } from '../../shared/api/apiClient'
import type { AuthenticatedUser } from '../auth/authTypes'

export interface UpdateUserProfileRequest {
  displayName: string
  profileImageUrl: string | null
}

export function updateUserProfile(request: UpdateUserProfileRequest) {
  return apiRequest<AuthenticatedUser>('/api/v1/users/me/profile', {
    method: 'PATCH',
    authenticated: true,
    body: JSON.stringify(request),
  })
}
