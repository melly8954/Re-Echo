import type { KeyboardEvent as ReactKeyboardEvent, PropsWithChildren } from 'react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import styles from './AppShell.module.css'

interface ChannelNavigationItem {
  id: string
  name: string
  href: string
}

interface AppShellProps {
  workspaceName?: string
  channels?: ChannelNavigationItem[]
  activeChannelId?: string
}

export function AppShell({
  children,
  workspaceName,
  channels = [],
  activeChannelId,
}: PropsWithChildren<AppShellProps>) {
  const { user, logout } = useAuth()
  const [isChannelDrawerOpen, setIsChannelDrawerOpen] = useState(false)
  const channelMenuButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerRef = useRef<HTMLElement>(null)
  const channelDrawerCloseButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerId = 'mobile-channel-drawer'

  useEffect(() => {
    if (!isChannelDrawerOpen) {
      return undefined
    }

    channelDrawerCloseButtonRef.current?.focus()
    const channelMenuButton = channelMenuButtonRef.current
    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsChannelDrawerOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
      channelMenuButton?.focus()
    }
  }, [isChannelDrawerOpen])

  function handleChannelDrawerKeyDown(event: ReactKeyboardEvent<HTMLElement>) {
    if (event.key !== 'Tab') {
      return
    }

    const focusableElements = channelDrawerRef.current?.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled])',
    )
    if (!focusableElements || focusableElements.length === 0) {
      return
    }

    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]
    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault()
      lastElement.focus()
      return
    }
    if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault()
      firstElement.focus()
    }
  }

  const channelNavigation = (
    <>
      {channels.length > 0 ? (
        <nav className={styles.channelList} aria-label="워크스페이스 채널">
          {channels.map((channel) => (
            <Link
              key={channel.id}
              className={
                channel.id === activeChannelId
                  ? `${styles.channelLink} ${styles.channelLinkActive}`
                  : styles.channelLink
              }
              to={channel.href}
            >
              <span aria-hidden="true">#</span>
              {channel.name}
            </Link>
          ))}
        </nav>
      ) : (
        <p className={styles.emptyText}>
          워크스페이스에 참여하면 채널이 표시됩니다.
        </p>
      )}
    </>
  )

  const channelList = (
    <div>
      <p className={styles.sidebarLabel}>채널</p>
      {channelNavigation}
    </div>
  )

  return (
    <div className={styles.shell}>
      <header className={styles.topBar}>
        <Link className={styles.brand} to="/" aria-label="Re-Echo 홈">
          <span aria-hidden="true">R</span>
          Re-Echo
        </Link>
        <p className={styles.workspaceName}>
          {workspaceName ?? '워크스페이스를 선택하세요'}
        </p>
        <div className={styles.account}>
          <button
            ref={channelMenuButtonRef}
            className={styles.channelMenuButton}
            type="button"
            aria-controls={channelDrawerId}
            aria-expanded={isChannelDrawerOpen}
            onClick={() => setIsChannelDrawerOpen(true)}
          >
            채널
          </button>
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
          {channelList}
        </aside>
        <main className={styles.content}>{children}</main>
      </div>

      {isChannelDrawerOpen ? (
        <div className={styles.mobileChannelLayer}>
          <button
            className={styles.mobileChannelOverlay}
            type="button"
            aria-label="채널 목록 닫기"
            onClick={() => setIsChannelDrawerOpen(false)}
          />
          <aside
            ref={channelDrawerRef}
            className={styles.mobileChannelDrawer}
            id={channelDrawerId}
            aria-label="모바일 채널 목록"
            onKeyDown={handleChannelDrawerKeyDown}
          >
            <div className={styles.mobileChannelHeader}>
              <p className={styles.sidebarLabel}>채널</p>
              <button
                ref={channelDrawerCloseButtonRef}
                className={styles.drawerCloseButton}
                type="button"
                onClick={() => setIsChannelDrawerOpen(false)}
              >
                닫기
              </button>
            </div>
            {channelNavigation}
          </aside>
        </div>
      ) : null}
    </div>
  )
}
