import { createContext } from 'react'
import type { AuthenticatedUser, AuthStatus } from './authTypes'

interface AuthContextValue {
  user: AuthenticatedUser | null
  status: AuthStatus
  retryAuthentication: () => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
