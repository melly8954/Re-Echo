import { AppBackdrop } from './AppBackdrop'
import styles from './AppLoadingScreen.module.css'

interface AppLoadingScreenProps {
  message: string
}

// 인증 복구나 초기 화면 데이터 준비 중 일관된 대기 화면을 표시한다.
export function AppLoadingScreen({ message }: AppLoadingScreenProps) {
  return (
    <main className={styles.page}>
      <AppBackdrop />
      <div className={styles.status} role="status">
        <span className={styles.spinner} aria-hidden="true" />
        <span>{message}</span>
      </div>
    </main>
  )
}
