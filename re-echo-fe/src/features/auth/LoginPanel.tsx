import { getOAuthLoginUrl, type OAuthProvider } from './authApi'
import styles from './LoginPanel.module.css'

const providers: Array<{
  id: OAuthProvider
  name: string
  mark: string
}> = [
  { id: 'google', name: 'Google', mark: 'G' },
  { id: 'kakao', name: 'Kakao', mark: 'K' },
  { id: 'github', name: 'GitHub', mark: 'GH' },
]

interface LoginPanelProps {
  hasAuthenticationError: boolean
  hasConnectionError: boolean
  onRetry: () => Promise<void>
}

// 인증 실패와 OAuth 공급자 선택을 로그인 화면의 한 패널로 제공한다.
export function LoginPanel({
  hasAuthenticationError,
  hasConnectionError,
  onRetry,
}: LoginPanelProps) {
  return (
    <section className={styles.panel} aria-labelledby="login-title">
      <div className={styles.brand} aria-hidden="true">
        R
      </div>
      <div className={styles.heading}>
        <p className={styles.eyebrow}>Re-Echo</p>
        <h1 id="login-title">팀의 대화를 한곳에서 이어가세요</h1>
        <p>
          워크스페이스와 채널에서 동료들과 빠르게 대화하고 파일을
          공유할 수 있습니다.
        </p>
      </div>

      {hasAuthenticationError && (
        <p className={styles.error} role="alert">
          로그인을 완료하지 못했습니다. 다시 시도해 주세요.
        </p>
      )}

      {hasConnectionError && (
        <div className={styles.error} role="alert">
          <span>서버에 연결할 수 없습니다.</span>
          <button type="button" onClick={() => void onRetry()}>
            다시 확인
          </button>
        </div>
      )}

      <div className={styles.providers} aria-label="소셜 로그인">
        {providers.map((provider) => (
          <a
            className={styles.providerButton}
            href={getOAuthLoginUrl(provider.id)}
            key={provider.id}
          >
            <span className={styles.providerMark} aria-hidden="true">
              {provider.mark}
            </span>
            {provider.name}로 계속하기
          </a>
        ))}
      </div>

      <p className={styles.notice}>
        로그인하면 Re-Echo의 서비스 이용 정책에 동의하게 됩니다.
      </p>
    </section>
  )
}
