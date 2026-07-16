import { createContext } from 'react'
import type { AuthenticatedUser, AuthStatus } from './authTypes'

// 인증 사용자와 세션 복구 동작을 하위 화면에 전달하는 Context 계약이다.
interface AuthContextValue {
  user: AuthenticatedUser | null
  status: AuthStatus
  retryAuthentication: () => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
