import { Navigate, useParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspacePage.module.css'

export function WorkspacePage() {
  const { workspaceId } = useParams()
  const workspaceQuery = useWorkspaceDetail(workspaceId ?? '')

  if (!workspaceId) {
    return <Navigate to="/" replace />
  }

  const workspace = workspaceQuery.data
  const defaultChannel = workspace
    ? {
        id: workspace.defaultChannelId,
        name: 'general',
        href: `/workspaces/${workspace.id}`,
      }
    : null

  return (
    <AppShell
      workspaceName={workspace?.name}
      channels={defaultChannel ? [defaultChannel] : []}
      activeChannelId={workspace?.defaultChannelId}
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

        {workspace && (
          <div className={styles.workspace}>
            <header className={styles.header}>
              <div>
                <p className={styles.eyebrow}># general</p>
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
                <strong>general</strong>
                <p>메시지 API가 연결되면 이 영역에 대화가 표시됩니다.</p>
              </div>
            </section>
          </div>
        )}
      </section>
    </AppShell>
  )
}
