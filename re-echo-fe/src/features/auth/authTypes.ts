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
