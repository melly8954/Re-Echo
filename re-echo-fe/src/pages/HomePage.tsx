import styles from './HomePage.module.css'

// 로그인 후 마지막 워크스페이스 또는 초기 진입 화면으로 연결한다.
export function HomePage() {
  return (
    <main className={styles.page}>
      <section className={styles.shell} aria-labelledby="home-title">
        <div className={styles.content}>
          <p className={styles.eyebrow}>Re-Echo</p>
          <h1 id="home-title">워크스페이스 협업 채팅</h1>
          <p className={styles.description}>
            React와 Vite 기반으로 시작하는 Re-Echo 프론트엔드입니다.
          </p>
        </div>

        <div className={styles.statusPanel} aria-label="프로젝트 구성 상태">
          <span>CSR SPA</span>
          <span>React Query</span>
          <span>CSS Module</span>
        </div>
      </section>
    </main>
  )
}
