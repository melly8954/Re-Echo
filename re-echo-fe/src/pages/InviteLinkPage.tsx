import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useAuth } from '../features/auth/useAuth'
import { useJoinWorkspaceInvite } from '../features/workspace/useJoinWorkspaceInvite'
import { useWorkspaceInvitePreview } from '../features/workspace/useWorkspaceInvitePreview'
import { ApiError } from '../shared/api/apiTypes'
import styles from './InviteLinkPage.module.css'

export function InviteLinkPage() {
  const { token } = useParams()
  const inviteToken = token ?? ''
  const navigate = useNavigate()
  const { status } = useAuth()
  const invitePreviewQuery = useWorkspaceInvitePreview(inviteToken)
  const joinInvite = useJoinWorkspaceInvite()

  if (!inviteToken) {
    return <Navigate to="/" replace />
  }

  async function handleJoinWorkspace() {
    try {
      const joinedWorkspace = await joinInvite.mutateAsync(inviteToken)
      void navigate(`/workspaces/${joinedWorkspace.id}`)
    } catch {
      // mutation 상태를 통해 오류 메시지를 화면에 표시한다.
    }
  }

  return (
    <AppShell>
      <main className={styles.page} aria-labelledby="invite-title">
        <section className={styles.panel}>
          {invitePreviewQuery.isLoading && (
            <>
              <p className={styles.eyebrow}>초대 확인</p>
              <h1 id="invite-title">초대 링크를 확인하고 있습니다</h1>
            </>
          )}

          {invitePreviewQuery.isError && (
            <>
              <p className={styles.eyebrow}>초대 오류</p>
              <h1 id="invite-title">초대 링크를 사용할 수 없습니다</h1>
              <p className={styles.description}>
                {invitePreviewQuery.error instanceof ApiError
                  ? invitePreviewQuery.error.message
                  : '초대 링크 상태를 확인하지 못했습니다.'}
              </p>
            </>
          )}

          {invitePreviewQuery.data && (
            <>
              <p className={styles.eyebrow}>워크스페이스 초대</p>
              <div className={styles.workspaceSummary}>
                {invitePreviewQuery.data.workspaceImageUrl ? (
                  <img src={invitePreviewQuery.data.workspaceImageUrl} alt="" />
                ) : (
                  <span aria-hidden="true">
                    {invitePreviewQuery.data.workspaceName.slice(0, 1)}
                  </span>
                )}
                <div>
                  <h1 id="invite-title">
                    {invitePreviewQuery.data.workspaceName}
                  </h1>
                  <p className={styles.description}>
                    이 워크스페이스에 참여합니다.
                  </p>
                </div>
              </div>

              {status === 'authenticated' ? (
                <button
                  type="button"
                  onClick={() => void handleJoinWorkspace()}
                  disabled={joinInvite.isPending}
                >
                  {joinInvite.isPending ? '참여 중...' : '참여하기'}
                </button>
              ) : (
                <button type="button" onClick={() => void navigate('/login')}>
                  로그인 후 참여하기
                </button>
              )}

              {joinInvite.isError && (
                <small className={styles.error} role="alert">
                  {joinInvite.error instanceof ApiError
                    ? joinInvite.error.message
                    : '워크스페이스에 참여하지 못했습니다.'}
                </small>
              )}
            </>
          )}
        </section>
      </main>
    </AppShell>
  )
}
