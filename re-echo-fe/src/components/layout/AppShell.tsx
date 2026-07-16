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
  canManage?: boolean
  canLeave?: boolean
}

interface AppShellProps {
  workspaceId?: string
  workspaceName?: string
  channels?: ChannelNavigationItem[]
  isWorkspaceHome?: boolean
  activeChannelId?: string
  isChannelsLoading?: boolean
  channelHeaderAction?: ReactNode
  onOpenChannelSettings?: (channelId: string) => void
  onRequestLeaveChannel?: (channelId: string) => void
  isChannelLeavePending?: boolean
  channelEmptyMessage?: string
  rightSidebar?: ReactNode
  rightSidebarLabel?: string
}

// 워크스페이스 레일과 채널 탐색, 전역 다이얼로그를 조합하는 앱 레이아웃이다.
export function AppShell({
  children,
  workspaceId,
  workspaceName,
  channels = [],
  isWorkspaceHome = false,
  activeChannelId,
  isChannelsLoading = false,
  channelHeaderAction,
  onOpenChannelSettings,
  onRequestLeaveChannel,
  isChannelLeavePending = false,
  channelEmptyMessage = '워크스페이스에 참여하면 채널이 표시됩니다.',
  rightSidebar,
  rightSidebarLabel,
}: PropsWithChildren<AppShellProps>) {
  const navigate = useNavigate()
  const { user, status, logout } = useAuth()
  const workspaceListQuery = useWorkspaceList(status === 'authenticated')
  const [isChannelDrawerOpen, setIsChannelDrawerOpen] = useState(false)
  const [isWorkspaceActionMenuOpen, setIsWorkspaceActionMenuOpen] = useState(false)
  const [isWorkspaceCreateOpen, setIsWorkspaceCreateOpen] = useState(false)
  const [isWorkspaceInviteOpen, setIsWorkspaceInviteOpen] = useState(false)
  const channelMenuButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerRef = useRef<HTMLElement>(null)
  const channelDrawerCloseButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerId = 'mobile-channel-drawer'
  const workspaces = workspaceListQuery.data?.contents ?? []
  const selectedWorkspace = workspaces.find((workspace) => workspace.id === workspaceId)
  const currentWorkspaceName = workspaceName ?? selectedWorkspace?.name ?? '워크스페이스'

  // 모바일 드로어가 열린 동안 포커스 시작점과 Esc 닫기, 배경 스크롤을 함께 제어한다.
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

  // Tab 이동이 드로어 바깥으로 빠져나가지 않도록 키보드 포커스를 순환시킨다.
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
    closeNavigationSurfaces()
    setIsWorkspaceCreateOpen(true)
  }

  function openWorkspaceInviteDialog() {
    closeNavigationSurfaces()
    setIsWorkspaceInviteOpen(true)
  }

  // 새 다이얼로그를 열기 전 겹칠 수 있는 모바일 탐색 표면을 모두 닫는다.
  function closeNavigationSurfaces() {
    setIsChannelDrawerOpen(false)
    setIsWorkspaceActionMenuOpen(false)
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
          {channels.map((channel) => {
            const isActiveChannel = channel.id === activeChannelId
            const hasChannelAction = (
              (channel.canManage && onOpenChannelSettings) ||
              (channel.canLeave && onRequestLeaveChannel)
            )

            return (
              <div
                key={channel.id}
                className={
                  isActiveChannel
                    ? `${styles.channelRow} ${styles.channelRowActive}`
                    : styles.channelRow
                }
              >
                <Link
                  className={
                    isActiveChannel
                      ? `${styles.channelLink} ${styles.channelLinkActive}`
                      : `${styles.channelLink} ${
                          channel.joined ? '' : styles.channelLinkUnjoined
                        }`
                  }
                  to={channel.href}
                  onClick={closeNavigationSurfaces}
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
                  <span className={`${styles.channelName} ${channel.unreadCount > 0 ? styles.channelNameUnread : ''}`}>
                    {channel.name}
                  </span>
                  {channel.unreadCount > 0 && (
                    <span className={styles.unreadBadge} aria-label={`읽지 않은 메시지 ${channel.unreadCount}개`}>
                      {channel.unreadCount > 99 ? '99+' : channel.unreadCount}
                    </span>
                  )}
                </Link>
                {hasChannelAction && (
                  <div className={styles.channelActions} aria-label={`${channel.name} 관리 동작`}>
                    {channel.canManage && onOpenChannelSettings && (
                      <button
                        className={styles.channelActionButton}
                        type="button"
                        aria-label="채널 설정"
                        title="채널 설정"
                        onClick={() => {
                          closeNavigationSurfaces()
                          onOpenChannelSettings(channel.id)
                        }}
                      >
                        <svg className={styles.channelSettingsIcon} viewBox="0 0 32 32" aria-hidden="true" focusable="false">
                          <circle className={styles.channelSettingsIconRing} cx="16" cy="16" r="14" />
                          <path className={styles.channelSettingsIconGear} d="M13 7h6l1 3.3 2.2 1 3-1.4 3.1 5.4-2.6 2 0 2.5 2.6 2-3.1 5.4-3-1.4-2.2 1-1 3.3h-6l-1-3.3-2.2-1-3 1.4-3.1-5.4 2.6-2v-2.5l-2.6-2 3.1-5.4 3 1.4 2.2-1L13 7Z" />
                          <circle className={styles.channelSettingsIconCenter} cx="16" cy="16" r="3.8" />
                        </svg>
                      </button>
                    )}
                    {channel.canLeave && onRequestLeaveChannel && (
                      <button
                        className={`${styles.channelActionButton} ${styles.channelLeaveActionButton}`}
                        type="button"
                        aria-label="채널 나가기"
                        title="채널 나가기"
                        onClick={() => {
                          closeNavigationSurfaces()
                          onRequestLeaveChannel(channel.id)
                        }}
                        disabled={isChannelLeavePending}
                      >
                        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
                          <path d="M4.5 3.5h9v17h-9v-17Z" />
                          <path d="M13.5 12h6m-2.5-2.5L19.5 12 17 14.5" />
                        </svg>
                      </button>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </nav>
      ) : (
        <p className={styles.emptyText}>
          {channelEmptyMessage}
        </p>
      )}
    </>
  )

  const workspaceRailContent = (
    <div className={styles.workspaceRailContent}>
      {workspaceListQuery.isLoading ? (
        <div className={styles.workspaceRailSkeleton} aria-label="워크스페이스 목록을 불러오는 중">
          <span />
          <span />
        </div>
      ) : workspaces.length > 0 ? (
        <nav className={styles.workspaceRailList} aria-label="워크스페이스 목록">
          {workspaces.map((workspace) => (
            <Link
              key={workspace.id}
              className={
                workspace.id === workspaceId
                  ? `${styles.workspaceRailLink} ${styles.workspaceRailLinkActive}`
                  : styles.workspaceRailLink
              }
              to={`/workspaces/${workspace.id}`}
              title={workspace.name}
              aria-label={workspace.name}
              onClick={closeNavigationSurfaces}
            >
              {workspace.imageUrl ? (
                <img src={workspace.imageUrl} alt="" />
              ) : (
                <span aria-hidden="true">{workspace.name.slice(0, 1)}</span>
              )}
            </Link>
          ))}
        </nav>
      ) : (
        <span className={styles.workspaceRailEmpty} aria-label="참여 중인 워크스페이스가 없습니다.">
          —
        </span>
      )}
      {workspaceListQuery.isError && (
        <button
          className={styles.workspaceRailRetryButton}
          type="button"
          onClick={() => void workspaceListQuery.refetch()}
        >
          다시 시도
        </button>
      )}
      <div className={styles.workspaceActionArea}>
        <button
          className={styles.workspaceActionButton}
          type="button"
          aria-expanded={isWorkspaceActionMenuOpen}
          aria-label="워크스페이스 메뉴"
          title="워크스페이스 메뉴"
          onClick={() => setIsWorkspaceActionMenuOpen((isOpen) => !isOpen)}
        >
          +
        </button>
        {isWorkspaceActionMenuOpen && (
          <div className={styles.workspaceActionMenu}>
            <button type="button" onClick={openWorkspaceCreateDialog}>
              새 워크스페이스 만들기
            </button>
            <button type="button" onClick={openWorkspaceInviteDialog}>
              초대 링크로 참여
            </button>
          </div>
        )}
      </div>
    </div>
  )

  const workspaceNavigation = workspaceId && (
    <Link
      className={
        isWorkspaceHome
          ? `${styles.workspaceHeaderLink} ${styles.workspaceHeaderLinkActive}`
          : styles.workspaceHeaderLink
      }
      to={`/workspaces/${workspaceId}`}
      onClick={closeNavigationSurfaces}
    >
      <span>{currentWorkspaceName}</span>
      <span aria-hidden="true">⌂</span>
    </Link>
  )

  const channelSidebarContent = (
    <div className={styles.channelSidebarContent}>
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
  const hasWorkspaceRail = status === 'authenticated'
  const hasChannelSidebar = Boolean(workspaceId)

  return (
    <div className={styles.shell}>
      <header className={styles.topBar}>
        <div className={styles.brand} aria-label="Re-Echo">
          <img src="/logo.png" alt="" aria-hidden="true" />
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
            : hasWorkspaceRail && hasChannelSidebar
              ? styles.body
              : hasWorkspaceRail
                ? `${styles.body} ${styles.bodyWithoutChannelSidebar}`
                : `${styles.body} ${styles.bodyWithoutSidebar}`
        }
      >
        {hasWorkspaceRail && (
          <aside className={styles.workspaceRail} aria-label="워크스페이스 탐색">
            {workspaceRailContent}
          </aside>
        )}
        {hasChannelSidebar && (
          <aside className={styles.channelSidebar} aria-label="현재 워크스페이스 채널 탐색">
            {channelSidebarContent}
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
              <p className={styles.sidebarLabel}>메뉴</p>
              <button
                ref={channelDrawerCloseButtonRef}
                className={styles.drawerCloseButton}
                type="button"
                onClick={() => setIsChannelDrawerOpen(false)}
              >
                닫기
              </button>
            </div>
            <div className={styles.mobileWorkspaceRail}>{workspaceRailContent}</div>
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
