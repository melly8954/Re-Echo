import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, type PropsWithChildren } from 'react'
import { setAuthenticationRefreshHandler } from '../../shared/api/apiClient'
import { ApiError } from '../../shared/api/apiTypes'
import { clearAccessToken, setAccessToken } from '../../stores/authStore'
import { getCurrentUser, logout, refreshToken } from './authApi'
import { AuthContext } from './authContext'
import { authSessionQueryKey } from './authQuery'
import type { AuthStatus } from './authTypes'

async function restoreSession() {
  try {
    const token = await refreshToken()
    setAccessToken(token.accessToken)
    return await getCurrentUser()
  } catch (error) {
    clearAccessToken()
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

// 앱 시작 시 Refresh Token으로 메모리 인증 상태를 복원한다.
export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient()
  const sessionQuery = useQuery({
    queryKey: authSessionQueryKey,
    queryFn: restoreSession,
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
  })

  useEffect(() => {
    setAuthenticationRefreshHandler(async () => {
      try {
        const token = await refreshToken()
        setAccessToken(token.accessToken)
        return true
      } catch {
        clearAccessToken()
        queryClient.setQueryData(authSessionQueryKey, null)
        return false
      }
    })

    return () => setAuthenticationRefreshHandler(null)
  }, [queryClient])

  let status: AuthStatus = 'loading'
  if (sessionQuery.isError) {
    status = 'error'
  } else if (sessionQuery.isSuccess) {
    status = sessionQuery.data ? 'authenticated' : 'unauthenticated'
  }

  async function retryAuthentication() {
    await sessionQuery.refetch()
  }

  async function endSession() {
    try {
      await logout()
    } finally {
      clearAccessToken()
      queryClient.setQueryData(authSessionQueryKey, null)
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user: sessionQuery.data ?? null,
        status,
        retryAuthentication,
        logout: endSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
