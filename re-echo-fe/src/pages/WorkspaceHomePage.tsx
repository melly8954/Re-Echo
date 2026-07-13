import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useIssueWorkspaceInviteLink } from '../features/workspace/useIssueWorkspaceInviteLink'
import { useLeaveWorkspace } from '../features/workspace/useLeaveWorkspace'
import { useWorkspaceChannels } from '../features/workspace/useWorkspaceChannels'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import {
  useWorkspaceInviteLink,
  workspaceInviteLinkQueryKey,
} from '../features/workspace/useWorkspaceInviteLink'
import { useWorkspaceMembers } from '../features/workspace/useWorkspaceMembers'
import { workspaceListQueryKey } from '../features/workspace/useWorkspaceList'
import {
  getWorkspaceRoleLabel,
  getWorkspaceStatusLabel,
} from '../features/workspace/workspaceLabels'
import type { WorkspaceInviteLink } from '../features/workspace/workspaceApi'
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
  const [inviteMessage, setInviteMessage] = useState<string | null>(null)
  const [isWorkspaceLeaveOpen, setIsWorkspaceLeaveOpen] = useState(false)
  const workspace = workspaceQuery.data
  const canIssueInvite =
    workspace?.myMembership.role === 'OWNER' ||
    workspace?.myMembership.role === 'ADMIN'
  const canLeaveWorkspace = Boolean(
    workspace && workspace.myMembership.role !== 'OWNER',
  )
  const inviteLinkQuery = useWorkspaceInviteLink(routeWorkspaceId, canIssueInvite)
  const channels = channelsQuery.data?.contents ?? []
  const members = workspaceMembersQuery.data?.contents ?? []
  const hasNoActiveInviteLink =
    inviteLinkQuery.error instanceof ApiError &&
    inviteLinkQuery.error.errorCode === 'INVITE_NOT_FOUND'

  useEffect(() => {
    if (!isWorkspaceLeaveOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !leaveWorkspace.isPending) {
        setIsWorkspaceLeaveOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [isWorkspaceLeaveOpen, leaveWorkspace.isPending])

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

  return (
    <AppShell
      workspaceId={workspaceId}
      workspaceName={workspace?.name}
      channels={channels.map((channel) => ({
        ...channel,
        href: `/workspaces/${workspaceId}/channels/${channel.id}`,
      }))}
      isChannelsLoading={channelsQuery.isLoading}
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
                  {workspace.description ?? '채널 현황과 팀 정보를 확인하세요.'}
                </p>
              </div>
              <div className={styles.actions}>
                <span>{getWorkspaceRoleLabel(workspace.myMembership.role)}</span>
                <span>{getWorkspaceStatusLabel(workspace.status)}</span>
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
            </header>

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
                {hasNoActiveInviteLink && (
                  <button
                    type="button"
                    onClick={() => void handleIssueInviteLink()}
                    disabled={issueInviteLink.isPending}
                  >
                    {issueInviteLink.isPending ? '발급 중' : '초대 링크 발급'}
                  </button>
                )}
                {inviteLinkQuery.isError && !hasNoActiveInviteLink && (
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
          </div>
        )}
      </section>
      {workspaceLeaveDialog}
    </AppShell>
  )
}
