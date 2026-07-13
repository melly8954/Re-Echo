import type {
  ChangeEvent,
  KeyboardEvent as ReactKeyboardEvent,
  PropsWithChildren,
  ReactNode,
} from 'react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
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
  activeChannelId?: string
  isChannelsLoading?: boolean
  channelHeaderAction?: ReactNode
  rightSidebar?: ReactNode
  rightSidebarLabel?: string
}

export function AppShell({
  children,
  workspaceId,
  workspaceName,
  channels = [],
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
  const channelMenuButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerRef = useRef<HTMLElement>(null)
  const channelDrawerCloseButtonRef = useRef<HTMLButtonElement>(null)
  const channelDrawerId = 'mobile-channel-drawer'
  const workspaces = workspaceListQuery.data?.contents ?? []
  const ownedWorkspaces = workspaces.filter((workspace) => workspace.role === 'OWNER')
  const joinedWorkspaces = workspaces.filter((workspace) => workspace.role !== 'OWNER')

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

  function handleWorkspaceChange(event: ChangeEvent<HTMLSelectElement>) {
    const nextWorkspaceId = event.target.value
    if (!nextWorkspaceId || nextWorkspaceId === workspaceId) {
      return
    }

    void navigate(`/workspaces/${nextWorkspaceId}`)
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
                {channel.visibility === 'PRIVATE' ? 'private' : '#'}
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

  const channelList = (
    <div>
      <div className={styles.channelHeader}>
        <p className={styles.sidebarLabel}>채널</p>
        {channelHeaderAction}
      </div>
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
        <div className={styles.workspaceSelector}>
          <select
            value={workspaceId ?? ''}
            aria-label="워크스페이스 선택"
            title={workspaceName ?? '워크스페이스를 선택하세요'}
            disabled={workspaceListQuery.isLoading || workspaceListQuery.isError}
            onChange={handleWorkspaceChange}
          >
            <option value="" disabled>
              {workspaceListQuery.isLoading
                ? '워크스페이스 불러오는 중'
                : workspaceListQuery.isError
                  ? '워크스페이스 목록 오류'
                  : '워크스페이스를 선택하세요'}
            </option>
            {ownedWorkspaces.length > 0 && (
              <optgroup label="내가 만든 워크스페이스">
                {ownedWorkspaces.map((workspace) => (
                  <option key={workspace.id} value={workspace.id}>
                    {workspace.name}
                  </option>
                ))}
              </optgroup>
            )}
            {joinedWorkspaces.length > 0 && (
              <optgroup label="참여 중인 워크스페이스">
                {joinedWorkspaces.map((workspace) => (
                  <option key={workspace.id} value={workspace.id}>
                    {workspace.name}
                  </option>
                ))}
              </optgroup>
            )}
          </select>
          {workspaceListQuery.isError && (
            <button type="button" onClick={() => void workspaceListQuery.refetch()}>
              다시 시도
            </button>
          )}
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

      <div
        className={
          rightSidebar ? `${styles.body} ${styles.bodyWithRightSidebar}` : styles.body
        }
      >
        <aside className={styles.sidebar} aria-label="채널 목록">
          {channelList}
        </aside>
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
            aria-label="모바일 채널 목록"
            onKeyDown={handleChannelDrawerKeyDown}
          >
            <div className={styles.mobileChannelHeader}>
              <div className={styles.mobileChannelTitle}>
                <p className={styles.sidebarLabel}>채널</p>
                {channelHeaderAction}
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
            {channelNavigation}
          </aside>
        </div>
      ) : null}
    </div>
  )
}
