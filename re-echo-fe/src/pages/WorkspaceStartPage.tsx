import { AppShell } from '../components/layout/AppShell'
import styles from './WorkspaceStartPage.module.css'

export function WorkspaceStartPage() {
  return (
    <AppShell>
      <section className={styles.page} aria-labelledby="workspace-start-title">
        <div className={styles.heading}>
          <p>시작하기</p>
          <h1 id="workspace-start-title">함께 대화할 공간을 준비하세요</h1>
          <span>
            새 워크스페이스를 만들거나 전달받은 초대 링크로 참여할 수
            있습니다.
          </span>
        </div>

        <div className={styles.options}>
          <article className={styles.card}>
            <span className={styles.icon} aria-hidden="true">
              +
            </span>
            <h2>새 워크스페이스 만들기</h2>
            <p>팀 이름을 정하고 첫 번째 채널에서 대화를 시작합니다.</p>
            <button type="button" disabled aria-describedby="create-help">
              워크스페이스 만들기
            </button>
            <small id="create-help">워크스페이스 기능 구현 후 연결됩니다.</small>
          </article>

          <article className={styles.card}>
            <span className={styles.icon} aria-hidden="true">
              ↗
            </span>
            <h2>초대 링크로 참여</h2>
            <p>팀에서 전달받은 초대 링크를 확인하고 참여합니다.</p>
            <button type="button" disabled aria-describedby="join-help">
              초대 링크 입력하기
            </button>
            <small id="join-help">초대 기능 구현 후 연결됩니다.</small>
          </article>
        </div>
      </section>
    </AppShell>
  )
}
