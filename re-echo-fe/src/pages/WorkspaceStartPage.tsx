import { Navigate } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useWorkspaceList } from '../features/workspace/useWorkspaceList'
import styles from './WorkspaceStartPage.module.css'

// 최종 방문 워크스페이스로 복귀시키고, 없을 때만 온보딩 빈 상태를 표시한다.
export function WorkspaceStartPage() {
  const workspaceListQuery = useWorkspaceList()
  const workspaces = workspaceListQuery.data?.contents ?? []
  const latestActiveWorkspace = workspaces.find((workspace) => workspace.status === 'ACTIVE')

  if (latestActiveWorkspace) {
    return <Navigate to={`/workspaces/${latestActiveWorkspace.id}`} replace />
  }

  return (
    <AppShell>
      <section className={styles.page} aria-labelledby="workspace-start-title">
        {workspaceListQuery.isLoading && <p className={styles.statusPanel}>워크스페이스를 확인하는 중입니다.</p>}
        {workspaceListQuery.isError && (
          <div className={styles.statusPanel}>
            <h1 id="workspace-start-title">워크스페이스를 불러올 수 없습니다.</h1>
            <button type="button" onClick={() => void workspaceListQuery.refetch()}>다시 시도</button>
          </div>
        )}
        {!workspaceListQuery.isLoading && !workspaceListQuery.isError && !latestActiveWorkspace && (
          <div className={styles.statusPanel}>
            <h1 id="workspace-start-title">아직 활성 워크스페이스가 없습니다.</h1>
            <p>왼쪽 메뉴에서 새 워크스페이스를 만들거나 초대 링크로 참여해 주세요.</p>
          </div>
        )}
      </section>
    </AppShell>
  )
}
