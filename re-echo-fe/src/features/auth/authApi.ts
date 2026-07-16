import { apiRequest, getApiUrl } from '../../shared/api/apiClient'
import type { AuthenticatedUser, AuthTokenResponse } from './authTypes'

export type OAuthProvider = 'google' | 'kakao' | 'github'

// OAuth 이동, 토큰 갱신, 현재 사용자 조회에 쓰는 인증 API를 제공한다.
export function getOAuthLoginUrl(provider: OAuthProvider) {
  return getApiUrl(`/oauth2/authorization/${provider}`)
}

export function refreshToken() {
  return apiRequest<AuthTokenResponse>('/api/v1/auth/refresh', {
    method: 'POST',
  })
}

export function getCurrentUser() {
  return apiRequest<AuthenticatedUser>('/api/v1/users/me', {
    authenticated: true,
  })
}

export function logout() {
  return apiRequest<null>('/api/v1/auth/logout', {
    method: 'POST',
  })
}
