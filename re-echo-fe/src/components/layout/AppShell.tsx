import type { KeyboardEvent as ReactKeyboardEvent, PropsWithChildren, ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import { WorkspaceCreateDialog } from '../../features/workspace/WorkspaceCreateDialog'
import { WorkspaceInviteEntryDialog } from '../../features/workspace/WorkspaceInviteEntryDialog'
import { useWorkspaceList } from '../../features/workspace/useWorkspaceList'
import styles from './AppShell.module.css'

interface ChannelNavigationItem {
  id: string
  name: string
  href: string
  visibility: 'PUBLIC' | 'PRIVATE'
  joined: boolean
  unreadCount: number
}

interface AppShellProps {
  workspaceId?: string
  workspaceName?: string
  channels?: ChannelNavigationItem[]
  isWorkspaceHome?: boolean
  activeChannelId?: string
  isChannelsLoading?: boolean
  channelHeaderAction?: ReactNode
  rightSidebar?: ReactNode
  rightSidebarLabel?: string
}

export function AppShell({
  children,
  workspaceId,
  channels = [],
  isWorkspaceHome = false,
  activeChannelId,
  isChannelsLoading = false,
  channelHeaderAction,
  rightSidebar,
  rightSidebarLabel,
}: PropsWithChildren<AppShellProps>) {
  const navigate = useNavigate()
  const { user, status, logout } = useAuth()
  const workspaceListQuery = useWorkspaceList(status === 'authenticated')
  const [isChannelDrawerOpen, setIsChannelDrawerOpen] = useState(false)
  const [isWorkspaceCreateOpen, setIsWorkspaceCreateOpen] = useState(false)
  const [isWorkspaceInviteOpen, setIsWorkspaceInviteOpen] = useState(false)
  const channelMenuButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerRef = useRef<HTMLElement>(null)
  const channelDrawerCloseButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerId = 'mobile-channel-drawer'
  const workspaces = workspaceListQuery.data?.contents ?? []

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
      'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled])',
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

  function openWorkspaceCreateDialog() {
    setIsChannelDrawerOpen(false)
    setIsWorkspaceCreateOpen(true)
  }

  function openWorkspaceInviteDialog() {
    setIsChannelDrawerOpen(false)
    setIsWorkspaceInviteOpen(true)
  }

  const channelNavigation = (
    <>
      {isChannelsLoading ? (
        <div className={styles.channelSkeletonList} aria-label="채널 목록을 불러오는 중">
          <span />
          <span />
          <span />
        </div>
      ) : channels.length > 0 ? (
        <nav className={styles.channelList} aria-label="워크스페이스 채널">
          {channels.map((channel) => (
            <Link
              key={channel.id}
              className={
                channel.id === activeChannelId
                  ? `${styles.channelLink} ${styles.channelLinkActive}`
                  : `${styles.channelLink} ${
                      channel.joined ? '' : styles.channelLinkUnjoined
                    }`
              }
              to={channel.href}
              onClick={() => setIsChannelDrawerOpen(false)}
            >
              <span className={styles.channelPrefix} aria-hidden="true">
                {channel.visibility === 'PRIVATE' ? (
                  <svg
                    className={styles.lockIcon}
                    viewBox="0 0 32 32"
                    focusable="false"
                  >
                    <path
                      fill="currentColor"
                      fillRule="evenodd"
                      d="M16 1.5c5.05 0 9.15 4.08 9.15 9.1v2.35h1.7c.91 0 1.65.74 1.65 1.65v11.45a4.45 4.45 0 0 1-4.45 4.45H7.95a4.45 4.45 0 0 1-4.45-4.45V14.6c0-.91.74-1.65 1.65-1.65h1.7V10.6c0-5.02 4.1-9.1 9.15-9.1Zm-6.1 11.45h2.65V10.6a3.45 3.45 0 0 1 6.9 0v2.35h2.65V10.6a6.1 6.1 0 0 0-12.2 0v2.35Zm-3.1 3.1v10c0 .64.51 1.15 1.15 1.15h16.1c.64 0 1.15-.51 1.15-1.15v-10H6.8Zm10.85 7.55.4-3.15a3 3 0 1 0-4.1 0l.4 3.15h3.3Z"
                    />
                  </svg>
                ) : (
                  '#'
                )}
              </span>
              <span className={styles.channelName}>{channel.name}</span>
              {channel.unreadCount > 0 && (
                <span className={styles.unreadBadge} aria-label={`읽지 않은 메시지 ${channel.unreadCount}개`}>
                  {channel.unreadCount}
                </span>
              )}
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

  const workspaceList = (
    <section className={styles.workspaceListSection} aria-labelledby="workspace-list-title">
      <p id="workspace-list-title" className={styles.sidebarLabel}>
        워크스페이스
      </p>
      {workspaceListQuery.isLoading ? (
        <div className={styles.workspaceListSkeleton} aria-label="워크스페이스 목록을 불러오는 중">
          <span />
          <span />
        </div>
      ) : workspaces.length > 0 ? (
        <nav className={styles.workspaceList} aria-label="워크스페이스 목록">
          {workspaces.map((workspace) => (
            <Link
              key={workspace.id}
              className={
                workspace.id === workspaceId
                  ? `${styles.workspaceLink} ${styles.workspaceLinkActive}`
                  : styles.workspaceLink
              }
              to={`/workspaces/${workspace.id}`}
              title={workspace.name}
              onClick={() => setIsChannelDrawerOpen(false)}
            >
              <span>{workspace.name}</span>
              {workspace.status === 'ARCHIVED' ? (
                <small>보관됨</small>
              ) : workspace.unreadChannelCount > 0 ? (
                <small aria-label={`읽지 않은 채널 ${workspace.unreadChannelCount}개`}>
                  {workspace.unreadChannelCount}
                </small>
              ) : null}
            </Link>
          ))}
        </nav>
      ) : (
        <p className={styles.emptyText}>참여 중인 워크스페이스가 없습니다.</p>
      )}
      {workspaceListQuery.isError && (
        <button type="button" onClick={() => void workspaceListQuery.refetch()}>
          다시 시도
        </button>
      )}
    </section>
  )

  const workspaceActions = (
    <div className={styles.workspaceActions}>
      <button type="button" onClick={openWorkspaceCreateDialog}>
        새 워크스페이스 만들기
      </button>
      <button type="button" onClick={openWorkspaceInviteDialog}>
        초대 링크로 참여
      </button>
    </div>
  )

  const workspaceNavigation = workspaceId && (
    <nav className={styles.workspaceNavigation} aria-label="워크스페이스 탐색">
      <Link
        className={
          isWorkspaceHome
            ? `${styles.workspaceHomeLink} ${styles.workspaceHomeLinkActive}`
            : styles.workspaceHomeLink
        }
        to={`/workspaces/${workspaceId}`}
        onClick={() => setIsChannelDrawerOpen(false)}
      >
        <span aria-hidden="true">⌂</span>
        홈
      </Link>
    </nav>
  )

  const channelList = (
    <div>
      {workspaceList}
      {workspaceActions}
      {workspaceNavigation}
      {workspaceId && (
        <>
          <div className={styles.channelHeader}>
            <p className={styles.sidebarLabel}>채널</p>
            {channelHeaderAction}
          </div>
          {channelNavigation}
        </>
      )}
    </div>
  )
  const hasWorkspaceNavigation = status === 'authenticated'

  return (
    <div className={styles.shell}>
      <header className={styles.topBar}>
        <div className={styles.brand} aria-label="Re-Echo">
          <span aria-hidden="true">R</span>
          Re-Echo
        </div>
        <div className={styles.account}>
          <button
            ref={channelMenuButtonRef}
            className={styles.channelMenuButton}
            type="button"
            aria-controls={channelDrawerId}
            aria-expanded={isChannelDrawerOpen}
            onClick={() => setIsChannelDrawerOpen(true)}
          >
            메뉴
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

      <div
        className={
          rightSidebar
            ? `${styles.body} ${styles.bodyWithRightSidebar}`
            : hasWorkspaceNavigation
              ? styles.body
              : `${styles.body} ${styles.bodyWithoutSidebar}`
        }
      >
        {hasWorkspaceNavigation && (
          <aside className={styles.sidebar} aria-label="워크스페이스 및 채널 탐색">
            {channelList}
          </aside>
        )}
        <main className={styles.content}>{children}</main>
        {rightSidebar && (
          <aside
            className={styles.rightSidebar}
            aria-label={rightSidebarLabel ?? '보조 패널'}
          >
            {rightSidebar}
          </aside>
        )}
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
            aria-label="모바일 워크스페이스 및 채널 메뉴"
            onKeyDown={handleChannelDrawerKeyDown}
          >
            <div className={styles.mobileChannelHeader}>
              <div className={styles.mobileChannelTitle}>
                <p className={styles.sidebarLabel}>메뉴</p>
                {workspaceId && channelHeaderAction}
              </div>
              <button
                ref={channelDrawerCloseButtonRef}
                className={styles.drawerCloseButton}
                type="button"
                onClick={() => setIsChannelDrawerOpen(false)}
              >
                닫기
              </button>
            </div>
            {workspaceList}
            {workspaceActions}
            {workspaceNavigation}
            {workspaceId && channelNavigation}
          </aside>
        </div>
      ) : null}
      <WorkspaceCreateDialog
        isOpen={isWorkspaceCreateOpen}
        onClose={() => setIsWorkspaceCreateOpen(false)}
        onCreated={(createdWorkspaceId) => {
          setIsWorkspaceCreateOpen(false)
          void navigate(`/workspaces/${createdWorkspaceId}`)
        }}
      />
      <WorkspaceInviteEntryDialog
        isOpen={isWorkspaceInviteOpen}
        onClose={() => setIsWorkspaceInviteOpen(false)}
      />
    </div>
  )
}
