import { useState } from 'react'
import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useGetWorkspaceInviteLink } from '../features/workspace/useGetWorkspaceInviteLink'
import { useIssueWorkspaceInviteLink } from '../features/workspace/useIssueWorkspaceInviteLink'
import { useWorkspaceChannels } from '../features/workspace/useWorkspaceChannels'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import {
  getWorkspaceMembershipStatusLabel,
  getWorkspaceRoleLabel,
  getWorkspaceStatusLabel,
} from '../features/workspace/workspaceLabels'
import { useWorkspaceMembers } from '../features/workspace/useWorkspaceMembers'
import type {
  WorkspaceInviteLink,
  WorkspaceMembershipRole,
} from '../features/workspace/workspaceApi'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspacePage.module.css'

export function WorkspacePage() {
  const { workspaceId } = useParams()
  const [searchParams] = useSearchParams()
  const getInviteLink = useGetWorkspaceInviteLink()
  const issueInviteLink = useIssueWorkspaceInviteLink()
  const workspaceQuery = useWorkspaceDetail(workspaceId ?? '')
  const channelsQuery = useWorkspaceChannels(workspaceId ?? '')
  const membersQuery = useWorkspaceMembers(workspaceId ?? '')
  const [inviteMessage, setInviteMessage] = useState<string | null>(null)

  if (!workspaceId) {
    return <Navigate to="/" replace />
  }

  const workspace = workspaceQuery.data
  const channels = channelsQuery.data?.contents ?? []
  const requestedChannelId = searchParams.get('channelId')
  const activeChannel =
    channels.find((channel) => channel.id === requestedChannelId) ??
    channels.find((channel) => channel.id === workspace?.defaultChannelId)
  const canIssueInvite =
    workspace?.myMembership.role === 'OWNER' ||
    workspace?.myMembership.role === 'ADMIN'
  const members = membersQuery.data?.contents ?? []
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

  async function handleCopyInviteLink() {
    if (!workspaceId) {
      return
    }

    setInviteMessage(null)
    try {
      let inviteLink: WorkspaceInviteLink
      try {
        inviteLink = await getInviteLink.mutateAsync(workspaceId)
      } catch (error) {
        if (!(error instanceof ApiError) || error.errorCode !== 'INVITE_NOT_FOUND') {
          throw error
        }
        inviteLink = await issueInviteLink.mutateAsync(workspaceId)
      }

      await copyInviteLink(inviteLink)
      setInviteMessage('초대 링크를 복사했습니다.')
    } catch (error) {
      if (error instanceof ApiError) {
        setInviteMessage(error.message)
        return
      }
      setInviteMessage('초대 링크를 복사하지 못했습니다.')
    }
  }

  async function copyInviteLink(inviteLink: WorkspaceInviteLink) {
    const inviteUrl = `${window.location.origin}/invite-links/${inviteLink.token}`
    await navigator.clipboard.writeText(inviteUrl)
  }

  const memberPanel = (
    <section className={styles.memberPanel} aria-labelledby="workspace-member-title">
      <div className={styles.memberPanelHeader}>
        <div>
          <p className={styles.eyebrow}>참여자</p>
          <h2 id="workspace-member-title">워크스페이스 참여자</h2>
        </div>
        <span>{members.length}</span>
      </div>

      {membersQuery.isLoading && (
        <div className={styles.memberSkeletonList} aria-label="참여자 목록을 불러오는 중">
          <span />
          <span />
          <span />
        </div>
      )}

      {membersQuery.isError && (
        <div className={styles.memberError}>
          <p>참여자 목록을 불러올 수 없습니다.</p>
          <button type="button" onClick={() => void membersQuery.refetch()}>
            다시 시도
          </button>
        </div>
      )}

      {!membersQuery.isLoading && !membersQuery.isError && (
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
      channels={channels.map((channel) => ({
        ...channel,
        href: `/workspaces/${workspaceId}?channelId=${channel.id}`,
      }))}
      activeChannelId={activeChannel?.id}
      isChannelsLoading={channelsQuery.isLoading}
      rightSidebar={workspace ? memberPanel : undefined}
      rightSidebarLabel="워크스페이스 참여자"
    >
      <section className={styles.page} aria-labelledby="workspace-page-title">
        {workspaceQuery.isLoading && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>불러오는 중</p>
            <h1 id="workspace-page-title">워크스페이스를 여는 중입니다</h1>
          </div>
        )}

        {workspaceQuery.isError && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>진입 실패</p>
            <h1 id="workspace-page-title">워크스페이스를 열 수 없습니다</h1>
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

        {channelsQuery.isError && workspace && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>채널 목록 오류</p>
            <h1 id="workspace-page-title">채널 목록을 불러올 수 없습니다</h1>
            <p className={styles.description}>
              {channelsQuery.error instanceof ApiError
                ? channelsQuery.error.message
                : '잠시 후 다시 시도해 주세요.'}
            </p>
            <button type="button" onClick={() => void channelsQuery.refetch()}>
              다시 시도
            </button>
          </div>
        )}

        {workspace && !channelsQuery.isError && (
          <div className={styles.workspace}>
            <header className={styles.header}>
              <div>
                <p className={styles.eyebrow}>
                  {activeChannel?.visibility === 'PRIVATE' ? '비공개' : '#'}{' '}
                  {activeChannel?.name ?? '채널'}
                </p>
                <h1 id="workspace-page-title">{workspace.name}</h1>
                <p className={styles.description}>
                  {workspace.description ?? '기본 채널에서 첫 대화를 시작하세요.'}
                </p>
              </div>
              <div className={styles.status}>
                <span>{getWorkspaceRoleLabel(workspace.myMembership.role)}</span>
                <span>{getWorkspaceStatusLabel(workspace.status)}</span>
                {canIssueInvite && (
                  <button
                    type="button"
                    onClick={() => void handleCopyInviteLink()}
                    disabled={getInviteLink.isPending || issueInviteLink.isPending}
                  >
                    {getInviteLink.isPending || issueInviteLink.isPending
                      ? '복사 중'
                      : '초대 링크 복사'}
                  </button>
                )}
              </div>
            </header>

            {inviteMessage && (
              <p className={styles.inviteMessage} role="status">
                {inviteMessage}
              </p>
            )}

            <section className={styles.messagePanel} aria-label="메시지 영역">
              <div>
                <strong>{activeChannel?.name ?? '채널'}</strong>
                <p>메시지 API가 연결되면 이 영역에 대화가 표시됩니다.</p>
              </div>
            </section>
          </div>
        )}
      </section>
    </AppShell>
  )
}
