import { Navigate, useSearchParams } from 'react-router-dom'
import { AppBackdrop } from '../components/layout/AppBackdrop'
import { LoginPanel } from '../features/auth/LoginPanel'
import { useAuth } from '../features/auth/useAuth'
import styles from './LoginPage.module.css'

// 인증 복구 상태와 OAuth 로그인 패널을 로그인 경로에서 조합한다.
export function LoginPage() {
  const [searchParams] = useSearchParams()
  const { status, retryAuthentication } = useAuth()

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  return (
    <main className={styles.page}>
      <AppBackdrop />
      <LoginPanel
        hasAuthenticationError={
          searchParams.get('errorCode') ===
          'AUTH_OAUTH_AUTHENTICATION_FAILED'
        }
        hasConnectionError={status === 'error'}
        onRetry={retryAuthentication}
      />
    </main>
  )
}
