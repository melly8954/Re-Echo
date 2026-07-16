import styles from './AppBackdrop.module.css'

// 다이얼로그 뒤쪽을 가려 현재 상호작용 대상에 집중시키는 공통 배경이다.
export function AppBackdrop() {
  return (
    <div className={styles.backdrop} aria-hidden="true">
      <div className={styles.topBar}>
        <span className={styles.logoBlock} />
        <span className={styles.topLine} />
        <span className={styles.avatar} />
      </div>
      <div className={styles.body}>
        <aside className={styles.sidebar}>
          <span className={styles.sideTitle} />
          {Array.from({ length: 7 }, (_, index) => (
            <span className={styles.channel} key={index} />
          ))}
        </aside>
        <div className={styles.messages}>
          <span className={styles.messageTitle} />
          {Array.from({ length: 5 }, (_, index) => (
            <div className={styles.message} key={index}>
              <span className={styles.messageAvatar} />
              <span className={styles.messageLine} />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
