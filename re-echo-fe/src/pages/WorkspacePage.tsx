import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useWorkspaceChannels } from '../features/workspace/useWorkspaceChannels'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspacePage.module.css'

export function WorkspacePage() {
  const { workspaceId } = useParams()
  const [searchParams] = useSearchParams()
  const workspaceQuery = useWorkspaceDetail(workspaceId ?? '')
  const channelsQuery = useWorkspaceChannels(workspaceId ?? '')

  if (!workspaceId) {
    return <Navigate to="/" replace />
  }

  const workspace = workspaceQuery.data
  const channels = channelsQuery.data?.contents ?? []
  const requestedChannelId = searchParams.get('channelId')
  const activeChannel =
    channels.find((channel) => channel.id === requestedChannelId) ??
    channels.find((channel) => channel.id === workspace?.defaultChannelId)

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
              </div>
            </header>

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
