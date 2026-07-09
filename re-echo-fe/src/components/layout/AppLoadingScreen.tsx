import { AppBackdrop } from './AppBackdrop'
import styles from './AppLoadingScreen.module.css'

interface AppLoadingScreenProps {
  message: string
}

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
