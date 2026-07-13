import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './WorkspaceAccessDialog.module.css'

interface WorkspaceInviteEntryDialogProps {
  isOpen: boolean
  onClose: () => void
}

function decodeInviteToken(token: string) {
  try {
    return decodeURIComponent(token)
  } catch {
    return token
  }
}

function getInviteToken(input: string) {
  const trimmedInput = input.trim()
  const invitePath = '/invite-links/'

  if (!trimmedInput) {
    return ''
  }

  try {
    const inviteUrl = new URL(trimmedInput)
    const pathToken = inviteUrl.pathname.split(invitePath)[1]?.split('/')[0]
    return decodeInviteToken(pathToken ?? '')
  } catch {
    const pathToken = trimmedInput.includes(invitePath)
      ? trimmedInput.slice(trimmedInput.indexOf(invitePath) + invitePath.length)
      : trimmedInput
    return decodeInviteToken(pathToken.split(/[?#]/)[0].replace(/^\/+|\/+$/g, ''))
  }
}

// 전달받은 초대 링크나 토큰을 기존 초대 미리보기 흐름으로 연결한다.
export function WorkspaceInviteEntryDialog({ isOpen, onClose }: WorkspaceInviteEntryDialogProps) {
  const navigate = useNavigate()
  const [inviteInput, setInviteInput] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [isOpen, onClose])

  if (!isOpen) {
    return null
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const inviteToken = getInviteToken(inviteInput)
    if (!inviteToken) {
      setErrorMessage('초대 코드 또는 초대 링크를 입력해 주세요.')
      return
    }

    onClose()
    void navigate(`/invite-links/${encodeURIComponent(inviteToken)}`)
  }

  return (
    <div className={styles.layer}>
      <button className={styles.overlay} type="button" aria-label="초대 링크 참여 닫기" onClick={onClose} />
      <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="workspace-invite-entry-title">
        <header>
          <div>
            <p>워크스페이스 참여</p>
            <h2 id="workspace-invite-entry-title">초대 링크로 참여</h2>
          </div>
          <button type="button" onClick={onClose}>닫기</button>
        </header>
        <form onSubmit={handleSubmit}>
          <label>
            <span>초대 코드 또는 링크</span>
            <input
              value={inviteInput}
              autoFocus
              aria-invalid={Boolean(errorMessage)}
              aria-describedby={errorMessage ? 'workspace-invite-error' : 'workspace-invite-help'}
              onChange={(event) => {
                setInviteInput(event.target.value)
                setErrorMessage(null)
              }}
            />
            {errorMessage ? (
              <small id="workspace-invite-error" className={styles.error} role="alert">{errorMessage}</small>
            ) : (
              <small id="workspace-invite-help" className={styles.help}>복사한 초대 링크 전체를 붙여넣어도 됩니다.</small>
            )}
          </label>
          <footer>
            <button type="button" onClick={onClose}>취소</button>
            <button type="submit">초대 확인하기</button>
          </footer>
        </form>
      </section>
    </div>
  )
}
