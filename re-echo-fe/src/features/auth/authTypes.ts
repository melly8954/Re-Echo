// 인증 API 응답과 앱이 구분하는 세션 상태를 한 곳에서 정의한다.
export interface AuthTokenResponse {
  accessToken: string
  accessTokenExpiresAt: string
}

export interface AuthenticatedUser {
  id: string
  displayName: string
  profileImageUrl: string | null
  status: 'ACTIVE' | 'WITHDRAWN'
}

export type AuthStatus =
  | 'loading'
  | 'authenticated'
  | 'unauthenticated'
  | 'error'
