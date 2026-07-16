import { useContext } from 'react'
import { AuthContext } from './authContext'

// Provider가 관리하는 인증 상태를 화면과 기능 컴포넌트에 안전하게 제공한다.
export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth는 AuthProvider 안에서 사용해야 합니다.')
  }

  return context
}
