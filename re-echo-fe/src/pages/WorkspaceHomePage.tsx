import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useIssueWorkspaceInviteLink } from '../features/workspace/useIssueWorkspaceInviteLink'
import { useLeaveWorkspace } from '../features/workspace/useLeaveWorkspace'
import { useArchiveWorkspace } from '../features/workspace/useArchiveWorkspace'
import { useRestoreWorkspace } from '../features/workspace/useRestoreWorkspace'
import { WorkspaceChannelCreateDialog } from '../features/workspace/WorkspaceChannelCreateDialog'
import { WorkspaceMemberManagementDialog } from '../features/workspace/WorkspaceMemberManagementDialog'
import { WorkspaceProfileDialog } from '../features/workspace/WorkspaceProfileDialog'
import { WorkspaceSettingsDialog } from '../features/workspace/WorkspaceSettingsDialog'
import {
  useWorkspaceChannels,
  workspaceChannelsQueryKey,
} from '../features/workspace/useWorkspaceChannels'
import {
  useWorkspaceDetail,
  workspaceDetailQueryKey,
} from '../features/workspace/useWorkspaceDetail'
import {
  useWorkspaceInviteLink,
  workspaceInviteLinkQueryKey,
} from '../features/workspace/useWorkspaceInviteLink'
import { useWorkspaceMembers } from '../features/workspace/useWorkspaceMembers'
import { workspaceListQueryKey } from '../features/workspace/useWorkspaceList'
import { getWorkspaceMembershipStatusLabel } from '../features/workspace/workspaceLabels'
import type {
  WorkspaceInviteLink,
  WorkspaceMembershipRole,
} from '../features/workspace/workspaceApi'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspaceHomePage.module.css'

// 워크스페이스 범위의 정보와 관리 동작을 한 화면에 제공한다.
export function WorkspaceHomePage() {
  const { workspaceId } = useParams()
  const routeWorkspaceId = workspaceId ?? ''
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const workspaceQuery = useWorkspaceDetail(routeWorkspaceId)
  const channelsQuery = useWorkspaceChannels(routeWorkspaceId)
  const workspaceMembersQuery = useWorkspaceMembers(routeWorkspaceId)
  const issueInviteLink = useIssueWorkspaceInviteLink()
  const leaveWorkspace = useLeaveWorkspace()
  const archiveWorkspace = useArchiveWorkspace()
  const restoreWorkspace = useRestoreWorkspace()
  const [inviteMessage, setInviteMessage] = useState<string | null>(null)
  const [isWorkspaceLeaveOpen, setIsWorkspaceLeaveOpen] = useState(false)
  const [isWorkspaceArchiveOpen, setIsWorkspaceArchiveOpen] = useState(false)
  const [isWorkspaceRestoreOpen, setIsWorkspaceRestoreOpen] = useState(false)
  const [isChannelCreateOpen, setIsChannelCreateOpen] = useState(false)
  const [isMemberManagementOpen, setIsMemberManagementOpen] = useState(false)
  const [isWorkspaceProfileOpen, setIsWorkspaceProfileOpen] = useState(false)
  const [isWorkspaceSettingsOpen, setIsWorkspaceSettingsOpen] = useState(false)
  const workspace = workspaceQuery.data
  const isArchivedWorkspace = workspace?.status === 'ARCHIVED'
  const canIssueInvite = !isArchivedWorkspace && (
    workspace?.myMembership.role === 'OWNER' ||
    workspace?.myMembership.role === 'ADMIN'
  )
  const canManageWorkspaceSettings = canIssueInvite
  const canArchiveWorkspace = Boolean(
    workspace && !isArchivedWorkspace && workspace.myMembership.role === 'OWNER',
  )
  const canRestoreWorkspace = Boolean(
    workspace && isArchivedWorkspace && workspace.canRestore && workspace.myMembership.role === 'OWNER',
  )
  const canLeaveWorkspace = Boolean(
    workspace && workspace.myMembership.role !== 'OWNER',
  )
  const canUpdateWorkspaceProfile = Boolean(workspace && !isArchivedWorkspace)
  const canCreateChannel = canIssueInvite
  const inviteLinkQuery = useWorkspaceInviteLink(routeWorkspaceId, canIssueInvite)
  const channels = channelsQuery.data?.contents ?? []
  const members = workspaceMembersQuery.data?.contents ?? []
  const memberGroups: Array<{
    role: WorkspaceMembershipRole
    label: string
    members: typeof members
  }> = [
    {
      role: 'OWNER',
      label: '소유자',
      members: members.filter((member) => member.role === 'OWNER'),
    },
    {
      role: 'ADMIN',
      label: '관리자',
      members: members.filter((member) => member.role === 'ADMIN'),
    },
    {
      role: 'MEMBER',
      label: '멤버',
      members: members.filter((member) => member.role === 'MEMBER'),
    },
  ]
  const requiresInviteIssue =
    inviteLinkQuery.error instanceof ApiError &&
    (inviteLinkQuery.error.errorCode === 'INVITE_NOT_FOUND' ||
      inviteLinkQuery.error.errorCode === 'INVITE_EXPIRED')

  useEffect(() => {
    if (!isWorkspaceLeaveOpen && !isWorkspaceArchiveOpen && !isWorkspaceRestoreOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !leaveWorkspace.isPending && !archiveWorkspace.isPending && !restoreWorkspace.isPending) {
        setIsWorkspaceLeaveOpen(false)
        setIsWorkspaceArchiveOpen(false)
        setIsWorkspaceRestoreOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [
    archiveWorkspace.isPending,
    isWorkspaceArchiveOpen,
    isWorkspaceLeaveOpen,
    isWorkspaceRestoreOpen,
    leaveWorkspace.isPending,
    restoreWorkspace.isPending,
  ])

  if (!workspaceId) {
    return <Navigate to="/" replace />
  }

  const inviteUrl = inviteLinkQuery.data
    ? `${window.location.origin}/invite-links/${inviteLinkQuery.data.token}`
    : null

  async function handleIssueInviteLink() {
    setInviteMessage(null)
    try {
      const issuedInviteLink = await issueInviteLink.mutateAsync(routeWorkspaceId)
      queryClient.setQueryData<WorkspaceInviteLink>(
        workspaceInviteLinkQueryKey(routeWorkspaceId),
        issuedInviteLink,
      )
    } catch (error) {
      setInviteMessage(
        error instanceof ApiError
          ? error.message
          : '초대 링크를 발급하지 못했습니다.',
      )
    }
  }

  async function handleCopyInviteLink() {
    if (!inviteUrl) {
      return
    }

    setInviteMessage(null)
    try {
      await navigator.clipboard.writeText(inviteUrl)
      setInviteMessage('초대 링크를 복사했습니다.')
    } catch {
      setInviteMessage('초대 링크를 복사하지 못했습니다.')
    }
  }

  async function handleLeaveWorkspace() {
    if (!workspace || !canLeaveWorkspace) {
      return
    }

    try {
      await leaveWorkspace.mutateAsync({
        workspaceId: routeWorkspaceId,
        memberId: workspace.myMembership.id,
      })
      await queryClient.invalidateQueries({ queryKey: workspaceListQueryKey })
      setIsWorkspaceLeaveOpen(false)
      void navigate('/', { replace: true })
    } catch {
      // mutation 상태를 통해 확인 모달에 오류 메시지를 표시한다.
    }
  }

  function closeWorkspaceLeaveDialog() {
    if (leaveWorkspace.isPending) {
      return
    }
    leaveWorkspace.reset()
    setIsWorkspaceLeaveOpen(false)
  }

  async function handleArchiveWorkspace() {
    if (!workspace || !canArchiveWorkspace) {
      return
    }

    try {
      await archiveWorkspace.mutateAsync(routeWorkspaceId)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: workspaceListQueryKey }),
        queryClient.invalidateQueries({ queryKey: workspaceDetailQueryKey(routeWorkspaceId) }),
        queryClient.invalidateQueries({ queryKey: workspaceChannelsQueryKey(routeWorkspaceId) }),
      ])
      setIsWorkspaceArchiveOpen(false)
      setIsWorkspaceSettingsOpen(false)
    } catch {
      // mutation 상태를 통해 확인 모달에 오류 메시지를 표시한다.
    }
  }

  async function handleRestoreWorkspace() {
    if (!workspace || !canRestoreWorkspace) {
      return
    }

    try {
      await restoreWorkspace.mutateAsync(routeWorkspaceId)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: workspaceListQueryKey }),
        queryClient.invalidateQueries({ queryKey: workspaceDetailQueryKey(routeWorkspaceId) }),
        queryClient.invalidateQueries({ queryKey: workspaceChannelsQueryKey(routeWorkspaceId) }),
      ])
      setIsWorkspaceRestoreOpen(false)
    } catch {
      // mutation 상태를 통해 확인 모달에 오류 메시지를 표시한다.
    }
  }

  function closeWorkspaceArchiveDialog() {
    if (archiveWorkspace.isPending) {
      return
    }
    archiveWorkspace.reset()
    setIsWorkspaceArchiveOpen(false)
  }

  function closeWorkspaceRestoreDialog() {
    if (restoreWorkspace.isPending) {
      return
    }
    restoreWorkspace.reset()
    setIsWorkspaceRestoreOpen(false)
  }

  const workspaceLeaveDialog = canLeaveWorkspace && isWorkspaceLeaveOpen && (
    <div className={styles.workspaceLeaveLayer}>
      <button
        className={styles.workspaceLeaveOverlay}
        type="button"
        aria-label="워크스페이스 나가기 닫기"
        onClick={closeWorkspaceLeaveDialog}
      />
      <section
        className={styles.workspaceLeaveDialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="workspace-leave-title"
        aria-describedby="workspace-leave-description"
      >
        <p className={styles.eyebrow}>워크스페이스 나가기</p>
        <h2 id="workspace-leave-title">정말 나가시겠습니까?</h2>
        <p id="workspace-leave-description">
          나가면 이 워크스페이스의 채널 목록에서 제외됩니다. 다시 참여하려면
          초대 링크가 필요합니다.
        </p>
        {leaveWorkspace.isError && (
          <p className={styles.workspaceLeaveError} role="alert">
            {leaveWorkspace.error instanceof ApiError
              ? leaveWorkspace.error.message
              : '워크스페이스에서 나가지 못했습니다.'}
          </p>
        )}
        <div className={styles.workspaceLeaveFooter}>
          <button
            type="button"
            className={styles.workspaceLeaveCancelButton}
            onClick={closeWorkspaceLeaveDialog}
          >
            취소
          </button>
          <button
            type="button"
            className={styles.workspaceLeaveConfirmButton}
            onClick={() => void handleLeaveWorkspace()}
            disabled={leaveWorkspace.isPending}
          >
            {leaveWorkspace.isPending ? '나가는 중' : '나가기'}
          </button>
        </div>
      </section>
    </div>
  )

  const workspaceArchiveDialog = canArchiveWorkspace && isWorkspaceArchiveOpen && (
    <div className={styles.workspaceLifecycleLayer}>
      <button
        className={styles.workspaceLifecycleOverlay}
        type="button"
        aria-label="워크스페이스 보관 닫기"
        onClick={closeWorkspaceArchiveDialog}
      />
      <section
        className={styles.workspaceLifecycleDialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="workspace-archive-title"
        aria-describedby="workspace-archive-description"
      >
        <p className={styles.eyebrow}>워크스페이스 보관</p>
        <h2 id="workspace-archive-title">워크스페이스를 보관하시겠습니까?</h2>
        <p id="workspace-archive-description">
          모든 활성 채널이 읽기 전용으로 전환됩니다. 보관 후 15일 안에만 복원할 수 있습니다.
        </p>
        {archiveWorkspace.isError && (
          <p className={styles.workspaceLifecycleError} role="alert">
            {archiveWorkspace.error instanceof ApiError
              ? archiveWorkspace.error.message
              : '워크스페이스를 보관하지 못했습니다.'}
          </p>
        )}
        <div className={styles.workspaceLifecycleFooter}>
          <button type="button" onClick={closeWorkspaceArchiveDialog}>
            취소
          </button>
          <button
            type="button"
            className={styles.workspaceArchiveConfirmButton}
            onClick={() => void handleArchiveWorkspace()}
            disabled={archiveWorkspace.isPending}
          >
            {archiveWorkspace.isPending ? '보관 중' : '보관하기'}
          </button>
        </div>
      </section>
    </div>
  )

  const workspaceRestoreDialog = canRestoreWorkspace && isWorkspaceRestoreOpen && (
    <div className={styles.workspaceLifecycleLayer}>
      <button
        className={styles.workspaceLifecycleOverlay}
        type="button"
        aria-label="워크스페이스 복원 닫기"
        onClick={closeWorkspaceRestoreDialog}
      />
      <section
        className={styles.workspaceLifecycleDialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="workspace-restore-title"
        aria-describedby="workspace-restore-description"
      >
        <p className={styles.eyebrow}>워크스페이스 복원</p>
        <h2 id="workspace-restore-title">워크스페이스를 복원하시겠습니까?</h2>
        <p id="workspace-restore-description">
          워크스페이스와 보관으로 전환된 채널을 다시 활성화합니다.
        </p>
        {restoreWorkspace.isError && (
          <p className={styles.workspaceLifecycleError} role="alert">
            {restoreWorkspace.error instanceof ApiError
              ? restoreWorkspace.error.message
              : '워크스페이스를 복원하지 못했습니다.'}
          </p>
        )}
        <div className={styles.workspaceLifecycleFooter}>
          <button type="button" onClick={closeWorkspaceRestoreDialog}>
            취소
          </button>
          <button
            type="button"
            className={styles.workspaceRestoreConfirmButton}
            onClick={() => void handleRestoreWorkspace()}
            disabled={restoreWorkspace.isPending}
          >
            {restoreWorkspace.isPending ? '복원 중' : '복원하기'}
          </button>
        </div>
      </section>
    </div>
  )

  const channelCreateAction = canCreateChannel ? (
    <button
      className={styles.channelCreateActionButton}
      type="button"
      aria-label="채널 생성"
      title="채널 생성"
      onClick={() => setIsChannelCreateOpen(true)}
    >
      +
    </button>
  ) : undefined

  const workspaceMemberPanel = (
    <section className={styles.memberPanel} aria-labelledby="workspace-member-title">
      <div className={styles.memberPanelHeader}>
        <div>
          <p className={styles.eyebrow}>참여자</p>
          <h2 id="workspace-member-title">워크스페이스 참여자</h2>
          <p className={styles.memberPanelContext}>워크스페이스 전체</p>
        </div>
        <span>{members.length}</span>
      </div>

      {workspaceMembersQuery.isLoading && (
        <div className={styles.memberSkeletonList} aria-label="참여자 목록을 불러오는 중">
          <span />
          <span />
          <span />
        </div>
      )}

      {workspaceMembersQuery.isError && (
        <div className={styles.memberError}>
          <p>워크스페이스 참여자 목록을 불러올 수 없습니다.</p>
          <button type="button" onClick={() => void workspaceMembersQuery.refetch()}>
            다시 시도
          </button>
        </div>
      )}

      {!workspaceMembersQuery.isLoading && !workspaceMembersQuery.isError && (
        <div className={styles.memberGroups}>
          {memberGroups
            .filter((group) => group.members.length > 0)
            .map((group) => (
              <section key={group.role} className={styles.memberGroup}>
                <h3>
                  {group.label}
                  <span>{group.members.length}</span>
                </h3>
                <div className={styles.memberList}>
                  {group.members.map((member) => (
                    <div key={member.id} className={styles.memberItem}>
                      {member.profileImageUrl ? (
                        <img src={member.profileImageUrl} alt="" />
                      ) : (
                        <span className={styles.memberAvatarFallback} aria-hidden="true">
                          {member.displayName.slice(0, 1)}
                        </span>
                      )}
                      <div className={styles.memberContent}>
                        <div>
                          <strong>{member.displayName}</strong>
                          {member.id === workspace?.myMembership.id && <em>나</em>}
                        </div>
                        <small>{getWorkspaceMembershipStatusLabel(member.status)}</small>
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            ))}
        </div>
      )}
    </section>
  )

  return (
    <AppShell
      workspaceId={workspaceId}
      workspaceName={workspace?.name}
      isWorkspaceHome
      channelHeaderAction={channelCreateAction}
      channels={channels.map((channel) => ({
        ...channel,
        href: `/workspaces/${workspaceId}/channels/${channel.id}`,
      }))}
      channelEmptyMessage={
        isArchivedWorkspace
          ? '보관된 워크스페이스입니다. 채널은 복원 후 다시 표시됩니다.'
          : undefined
      }
      isChannelsLoading={channelsQuery.isLoading}
      rightSidebar={workspace ? workspaceMemberPanel : undefined}
      rightSidebarLabel="워크스페이스 참여자"
    >
      <section className={styles.page} aria-labelledby="workspace-home-title">
        {workspaceQuery.isLoading && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>불러오는 중</p>
            <h1 id="workspace-home-title">워크스페이스를 여는 중입니다</h1>
          </div>
        )}

        {workspaceQuery.isError && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>진입 실패</p>
            <h1 id="workspace-home-title">워크스페이스를 열 수 없습니다</h1>
            <p className={styles.description}>
              {workspaceQuery.error instanceof ApiError
                ? workspaceQuery.error.message
                : '잠시 후 다시 시도해 주세요.'}
            </p>
            <button type="button" onClick={() => void workspaceQuery.refetch()}>
              다시 시도
            </button>
          </div>
        )}

        {workspace && (
          <div className={styles.workspace}>
            <header className={styles.header}>
              <div>
                <p className={styles.eyebrow}>워크스페이스 홈</p>
                <h1 id="workspace-home-title">{workspace.name}</h1>
                <p className={styles.description}>
                  {workspace.description ?? (
                    isArchivedWorkspace
                      ? '워크스페이스가 보관되었습니다.'
                      : '채널 현황과 팀 정보를 확인하세요.'
                  )}
                </p>
              </div>
              {!isArchivedWorkspace && (
                <div className={styles.actions}>
                {canUpdateWorkspaceProfile && (
                  <button
                    type="button"
                    className={styles.memberManageButton}
                    onClick={() => setIsWorkspaceProfileOpen(true)}
                  >
                    내 프로필
                  </button>
                )}
                {canManageWorkspaceSettings && (
                  <button
                    type="button"
                    className={styles.memberManageButton}
                    onClick={() => setIsWorkspaceSettingsOpen(true)}
                  >
                    워크스페이스 설정
                  </button>
                )}
                {canIssueInvite && (
                  <button
                    type="button"
                    className={styles.memberManageButton}
                    onClick={() => setIsMemberManagementOpen(true)}
                  >
                    멤버 관리
                  </button>
                )}
                {canLeaveWorkspace && (
                  <button
                    type="button"
                    className={styles.workspaceLeaveButton}
                    onClick={() => {
                      leaveWorkspace.reset()
                      setIsWorkspaceLeaveOpen(true)
                    }}
                  >
                    워크스페이스 나가기
                  </button>
                )}
                </div>
              )}
            </header>

            {isArchivedWorkspace ? (
              <section className={styles.archivedWorkspacePanel} aria-labelledby="archived-workspace-title">
                <p className={styles.eyebrow}>보관됨</p>
                <h2 id="archived-workspace-title">이 워크스페이스는 보관 중입니다.</h2>
                <p>
                  채널은 보관 기간 동안 표시되지 않습니다. 소유자는 15일 안에
                  워크스페이스를 복원할 수 있습니다.
                </p>
                {canRestoreWorkspace && (
                  <button
                    type="button"
                    onClick={() => {
                      restoreWorkspace.reset()
                      setIsWorkspaceRestoreOpen(true)
                    }}
                  >
                    워크스페이스 복원
                  </button>
                )}
              </section>
            ) : (
              <>
                <section className={styles.summaryGrid} aria-label="워크스페이스 현황">
                  <article>
                    <span>전체 참여자</span>
                    <strong>{workspaceMembersQuery.isLoading ? '—' : members.length}</strong>
                  </article>
                  <article>
                    <span>채널</span>
                    <strong>{channelsQuery.isLoading ? '—' : channels.length}</strong>
                  </article>
                </section>

                {canIssueInvite && (
                  <section className={styles.invitePanel} aria-labelledby="invite-link-title">
                    <div>
                      <p className={styles.eyebrow}>팀 초대</p>
                      <h2 id="invite-link-title">초대 링크</h2>
                      <p>워크스페이스 전체에 참여할 수 있는 링크입니다.</p>
                    </div>
                    {inviteLinkQuery.isLoading && <span>초대 링크를 불러오는 중입니다.</span>}
                    {requiresInviteIssue && (
                      <button
                        type="button"
                        onClick={() => void handleIssueInviteLink()}
                        disabled={issueInviteLink.isPending}
                      >
                        {issueInviteLink.isPending ? '발급 중' : '초대 링크 발급'}
                      </button>
                    )}
                    {inviteLinkQuery.isError && !requiresInviteIssue && (
                      <button type="button" onClick={() => void inviteLinkQuery.refetch()}>
                        초대 링크 다시 불러오기
                      </button>
                    )}
                    {inviteUrl && (
                      <div className={styles.inviteLinkField}>
                        <input aria-label="초대 링크" value={inviteUrl} readOnly />
                        <button type="button" onClick={() => void handleCopyInviteLink()}>
                          복사
                        </button>
                      </div>
                    )}
                    {inviteMessage && <p className={styles.inviteMessage}>{inviteMessage}</p>}
                  </section>
                )}

                <section className={styles.channelPanel} aria-labelledby="workspace-channel-title">
                  <div className={styles.sectionHeader}>
                    <div>
                      <p className={styles.eyebrow}>채널 현황</p>
                      <h2 id="workspace-channel-title">채널 목록</h2>
                    </div>
                  </div>
                  {channelsQuery.isError && (
                    <p className={styles.errorText}>채널 목록을 불러올 수 없습니다.</p>
                  )}
                  {!channelsQuery.isLoading && !channelsQuery.isError && channels.length === 0 && (
                    <p className={styles.emptyText}>표시할 채널이 없습니다.</p>
                  )}
                  <div className={styles.channelGrid}>
                    {channels.map((channel) => (
                      <Link
                        key={channel.id}
                        className={styles.channelCard}
                        to={`/workspaces/${workspaceId}/channels/${channel.id}`}
                      >
                        <div>
                          <strong>
                            {channel.visibility === 'PRIVATE' ? '잠금 ' : '#'}{channel.name}
                          </strong>
                          <span>{channel.visibility === 'PRIVATE' ? '비공개' : '공개'}</span>
                        </div>
                        <dl>
                          <div>
                            <dt>참여자</dt>
                            <dd>{channel.memberCount}명</dd>
                          </div>
                          <div>
                            <dt>읽지 않음</dt>
                            <dd>{channel.unreadCount}개</dd>
                          </div>
                        </dl>
                      </Link>
                    ))}
                  </div>
                </section>
              </>
            )}
          </div>
        )}
      </section>
      {workspaceLeaveDialog}
      {workspaceArchiveDialog}
      {workspaceRestoreDialog}
      {workspace && canUpdateWorkspaceProfile && (
        <WorkspaceProfileDialog
          workspace={workspace}
          isOpen={isWorkspaceProfileOpen}
          onClose={() => setIsWorkspaceProfileOpen(false)}
        />
      )}
      {workspace && canManageWorkspaceSettings && (
        <WorkspaceSettingsDialog
          workspace={workspace}
          isOpen={isWorkspaceSettingsOpen}
          onClose={() => setIsWorkspaceSettingsOpen(false)}
          onRequestArchive={canArchiveWorkspace ? () => {
            setIsWorkspaceSettingsOpen(false)
            archiveWorkspace.reset()
            setIsWorkspaceArchiveOpen(true)
          } : undefined}
        />
      )}
      {workspace && canManageWorkspaceSettings && (
        <WorkspaceMemberManagementDialog
          workspaceId={workspaceId}
          workspaceName={workspace.name}
          currentMembership={workspace.myMembership}
          isOpen={isMemberManagementOpen}
          onClose={() => setIsMemberManagementOpen(false)}
        />
      )}
      {canCreateChannel && (
        <WorkspaceChannelCreateDialog
          workspaceId={workspaceId}
          isOpen={isChannelCreateOpen}
          onClose={() => setIsChannelCreateOpen(false)}
          onCreated={(channelId) => {
            setIsChannelCreateOpen(false)
            void navigate(`/workspaces/${workspaceId}/channels/${channelId}`)
          }}
        />
      )}
    </AppShell>
  )
}
