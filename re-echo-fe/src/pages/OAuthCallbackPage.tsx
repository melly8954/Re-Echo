import { Navigate } from 'react-router-dom'
import { AppLoadingScreen } from '../components/layout/AppLoadingScreen'
import { useAuth } from '../features/auth/useAuth'
import styles from './OAuthCallbackPage.module.css'

export function OAuthCallbackPage() {
  const { status, retryAuthentication } = useAuth()

  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login?errorCode=AUTH_OAUTH_AUTHENTICATION_FAILED" replace />
  }

  if (status === 'error') {
    return (
      <main className={styles.page}>
        <section className={styles.errorPanel} aria-labelledby="callback-error-title">
          <h1 id="callback-error-title">로그인 상태를 확인하지 못했습니다</h1>
          <p>서버 연결을 확인한 뒤 다시 시도해 주세요.</p>
          <button type="button" onClick={() => void retryAuthentication()}>
            다시 확인
          </button>
        </section>
      </main>
    )
  }

  return <AppLoadingScreen message="로그인을 완료하고 있습니다." />
}
