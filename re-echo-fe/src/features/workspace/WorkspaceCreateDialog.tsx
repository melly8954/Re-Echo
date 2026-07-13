import { useEffect, useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useCreateWorkspace } from './useCreateWorkspace'
import { workspaceListQueryKey } from './useWorkspaceList'
import { ApiError } from '../../shared/api/apiTypes'
import styles from './WorkspaceAccessDialog.module.css'

interface ValidationErrorResult {
  fieldErrors?: Array<{
    field: string
    reason: string
  }>
}

interface WorkspaceCreateDialogProps {
  isOpen: boolean
  onClose: () => void
  onCreated: (workspaceId: string) => void
}

function getFieldError(error: unknown, field: string) {
  if (!(error instanceof ApiError)) {
    return null
  }

  const result = error.result as ValidationErrorResult | null
  return result?.fieldErrors?.find((fieldError) => fieldError.field === field)?.reason ?? null
}

// 어느 화면에서나 워크스페이스를 만들고 생성 직후 해당 홈으로 이동시킨다.
export function WorkspaceCreateDialog({
  isOpen,
  onClose,
  onCreated,
}: WorkspaceCreateDialogProps) {
  const queryClient = useQueryClient()
  const createWorkspace = useCreateWorkspace()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [clientError, setClientError] = useState<string | null>(null)
  const nameError = clientError ?? getFieldError(createWorkspace.error, 'name')

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !createWorkspace.isPending) {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [createWorkspace.isPending, isOpen, onClose])

  if (!isOpen) {
    return null
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedName = name.trim()

    setClientError(null)
    createWorkspace.reset()
    if (!trimmedName) {
      setClientError('워크스페이스 이름을 입력해 주세요.')
      return
    }

    try {
      const workspace = await createWorkspace.mutateAsync({
        name: trimmedName,
        description: description.trim() || null,
      })
      await queryClient.invalidateQueries({ queryKey: workspaceListQueryKey })
      onCreated(workspace.id)
    } catch {
      // mutation 상태로 오류 메시지를 표시한다.
    }
  }

  return (
    <div className={styles.layer}>
      <button className={styles.overlay} type="button" aria-label="워크스페이스 생성 닫기" onClick={onClose} />
      <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="workspace-create-title">
        <header>
          <div>
            <p>새 워크스페이스</p>
            <h2 id="workspace-create-title">워크스페이스 만들기</h2>
          </div>
          <button type="button" onClick={onClose} disabled={createWorkspace.isPending}>닫기</button>
        </header>
        <form onSubmit={(event) => void handleSubmit(event)}>
          <label>
            <span>워크스페이스 이름</span>
            <input
              value={name}
              maxLength={100}
              autoFocus
              aria-invalid={Boolean(nameError)}
              aria-describedby={nameError ? 'workspace-name-error' : undefined}
              onChange={(event) => {
                setName(event.target.value)
                setClientError(null)
                createWorkspace.reset()
              }}
            />
            {nameError && <small id="workspace-name-error" className={styles.error} role="alert">{nameError}</small>}
          </label>
          <label>
            <span>설명</span>
            <textarea value={description} maxLength={500} rows={3} onChange={(event) => setDescription(event.target.value)} />
          </label>
          {createWorkspace.isError && !nameError && (
            <p className={styles.error} role="alert">
              {createWorkspace.error instanceof ApiError ? createWorkspace.error.message : '워크스페이스를 만들지 못했습니다.'}
            </p>
          )}
          <footer>
            <button type="button" onClick={onClose} disabled={createWorkspace.isPending}>취소</button>
            <button type="submit" disabled={createWorkspace.isPending}>
              {createWorkspace.isPending ? '생성 중' : '워크스페이스 만들기'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}
