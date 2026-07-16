import { useEffect, useRef } from 'react'
import styles from './WorkspaceProfileEditContextMenu.module.css'

interface WorkspaceProfileEditContextMenuProps {
  position: { x: number; y: number } | null
  onClose: () => void
  onSelect: () => void
}

// 내 참여자 항목의 우클릭 동작을 프로필 설정 화면 이동으로 연결한다.
export function WorkspaceProfileEditContextMenu({
  position,
  onClose,
  onSelect,
}: WorkspaceProfileEditContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!position) {
      return undefined
    }

    function handlePointerDown(event: PointerEvent) {
      if (!menuRef.current?.contains(event.target as Node)) {
        onClose()
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose, position])

  if (!position) {
    return null
  }

  const left = Math.max(8, Math.min(position.x, window.innerWidth - 228))
  const top = Math.max(8, Math.min(position.y, window.innerHeight - 52))

  return (
    <div
      ref={menuRef}
      className={styles.menu}
      role="menu"
      style={{ left, top }}
    >
      <button
        type="button"
        role="menuitem"
        onClick={() => {
          onClose()
          onSelect()
        }}
      >
        워크스페이스 프로필 편집
      </button>
    </div>
  )
}
