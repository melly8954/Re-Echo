import type { PropsWithChildren } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import styles from './AppShell.module.css'

export function AppShell({ children }: PropsWithChildren) {
  const { user, logout } = useAuth()

  return (
    <div className={styles.shell}>
      <header className={styles.topBar}>
        <Link className={styles.brand} to="/" aria-label="Re-Echo 홈">
          <span aria-hidden="true">R</span>
          Re-Echo
        </Link>
        <p className={styles.workspaceName}>워크스페이스를 선택하세요</p>
        <div className={styles.account}>
          {user?.profileImageUrl ? (
            <img src={user.profileImageUrl} alt="" />
          ) : (
            <span className={styles.avatarFallback} aria-hidden="true">
              {user?.displayName.slice(0, 1) ?? 'R'}
            </span>
          )}
          <span className={styles.userName}>{user?.displayName}</span>
          <Link className={styles.accountLink} to="/settings/profile">
            프로필
          </Link>
          <button
            className={styles.accountButton}
            type="button"
            onClick={() => void logout()}
          >
            로그아웃
          </button>
        </div>
      </header>

      <div className={styles.body}>
        <aside className={styles.sidebar} aria-label="채널 목록">
          <div>
            <p className={styles.sidebarLabel}>채널</p>
            <p className={styles.emptyText}>
              워크스페이스에 참여하면 채널이 표시됩니다.
            </p>
          </div>
        </aside>
        <main className={styles.content}>{children}</main>
      </div>
    </div>
  )
}
