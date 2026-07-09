import { Navigate, useSearchParams } from 'react-router-dom'
import { AppBackdrop } from '../components/layout/AppBackdrop'
import { LoginPanel } from '../features/auth/LoginPanel'
import { useAuth } from '../features/auth/useAuth'
import styles from './LoginPage.module.css'

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
