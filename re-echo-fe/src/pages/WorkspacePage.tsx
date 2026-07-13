import { useState } from 'react'
import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useGetWorkspaceInviteLink } from '../features/workspace/useGetWorkspaceInviteLink'
import { useIssueWorkspaceInviteLink } from '../features/workspace/useIssueWorkspaceInviteLink'
import { useWorkspaceChannels } from '../features/workspace/useWorkspaceChannels'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import type { WorkspaceInviteLink } from '../features/workspace/workspaceApi'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspacePage.module.css'

export function WorkspacePage() {
  const { workspaceId } = useParams()
  const [searchParams] = useSearchParams()
  const getInviteLink = useGetWorkspaceInviteLink()
  const issueInviteLink = useIssueWorkspaceInviteLink()
  const workspaceQuery = useWorkspaceDetail(workspaceId ?? '')
  const channelsQuery = useWorkspaceChannels(workspaceId ?? '')
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

  return (
    <AppShell
      workspaceName={workspace?.name}
      channels={channels.map((channel) => ({
        ...channel,
        href: `/workspaces/${workspaceId}?channelId=${channel.id}`,
      }))}
      activeChannelId={activeChannel?.id}
      isChannelsLoading={channelsQuery.isLoading}
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
                  {activeChannel?.visibility === 'PRIVATE' ? 'private' : '#'}{' '}
                  {activeChannel?.name ?? '채널'}
                </p>
                <h1 id="workspace-page-title">{workspace.name}</h1>
                <p className={styles.description}>
                  {workspace.description ?? '기본 채널에서 첫 대화를 시작하세요.'}
                </p>
              </div>
              <div className={styles.status}>
                <span>{workspace.myMembership.role}</span>
                <span>{workspace.status}</span>
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
