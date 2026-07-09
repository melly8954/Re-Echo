import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { AppLoadingScreen } from '../components/layout/AppLoadingScreen'
import { useAuth } from '../features/auth/useAuth'

// 인증 확인이 끝난 사용자만 보호된 화면에 진입시킨다.
export function ProtectedRoute() {
  const location = useLocation()
  const { status } = useAuth()

  if (status === 'loading') {
    return <AppLoadingScreen message="로그인 상태를 확인하고 있습니다." />
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
