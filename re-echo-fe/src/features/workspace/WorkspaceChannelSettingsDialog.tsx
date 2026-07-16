import { useEffect, useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../shared/api/apiTypes'
import { useArchiveWorkspaceChannel } from './useArchiveWorkspaceChannel'
import { useRestoreWorkspaceChannel } from './useRestoreWorkspaceChannel'
import { workspaceChannelDetailQueryKey } from './useWorkspaceChannelDetail'
import { workspaceChannelsQueryKey } from './useWorkspaceChannels'
import { useUpdateWorkspaceChannel } from './useUpdateWorkspaceChannel'
import type { WorkspaceChannelDetail } from './workspaceApi'
import styles from './WorkspaceChannelSettingsDialog.module.css'

interface WorkspaceChannelSettingsDialogProps {
  workspaceId: string
  channel: WorkspaceChannelDetail
  isOpen: boolean
  onClose: () => void
}

// 채널 관리자가 정보 변경과 보관 수명주기를 한 곳에서 처리하도록 제공한다.
export function WorkspaceChannelSettingsDialog({
  workspaceId,
  channel,
  isOpen,
  onClose,
}: WorkspaceChannelSettingsDialogProps) {
  const queryClient = useQueryClient()
  const updateChannel = useUpdateWorkspaceChannel()
  const archiveChannel = useArchiveWorkspaceChannel()
  const restoreChannel = useRestoreWorkspaceChannel()
  const { reset: resetUpdateChannel } = updateChannel
  const { reset: resetArchiveChannel } = archiveChannel
  const { reset: resetRestoreChannel } = restoreChannel
  const [name, setName] = useState(channel.name)
  const [description, setDescription] = useState(channel.description ?? '')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isArchiveConfirmOpen, setIsArchiveConfirmOpen] = useState(false)
  const isSaving = updateChannel.isPending || archiveChannel.isPending || restoreChannel.isPending

  useEffect(() => {
    if (!isOpen) {
      return
    }
    setName(channel.name)
    setDescription(channel.description ?? '')
    setErrorMessage(null)
    setIsArchiveConfirmOpen(false)
    resetUpdateChannel()
    resetArchiveChannel()
    resetRestoreChannel()
  }, [channel, isOpen, resetArchiveChannel, resetRestoreChannel, resetUpdateChannel])

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) {
        if (isArchiveConfirmOpen) {
          setIsArchiveConfirmOpen(false)
          return
        }
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isArchiveConfirmOpen, isOpen, isSaving, onClose])

  if (!isOpen) {
    return null
  }

  const trimmedName = name.trim()
  const normalizedDescription = description.trim() || null
  const isArchived = channel.status === 'ARCHIVED'
  const isUnchanged = trimmedName === channel.name && normalizedDescription === channel.description

  async function refreshChannelData() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: workspaceChannelsQueryKey(workspaceId) }),
      queryClient.invalidateQueries({
        queryKey: workspaceChannelDetailQueryKey(workspaceId, channel.id),
      }),
    ])
  }

  function toErrorMessage(error: unknown, fallback: string) {
    return error instanceof ApiError ? error.message : fallback
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!trimmedName) {
      setErrorMessage('채널 이름을 입력해 주세요.')
      return
    }
    setErrorMessage(null)
    try {
      await updateChannel.mutateAsync({
        workspaceId,
        channelId: channel.id,
        request: { name: trimmedName, description: normalizedDescription },
      })
      await refreshChannelData()
      onClose()
    } catch (error) {
      setErrorMessage(toErrorMessage(error, '채널 정보를 저장하지 못했습니다.'))
    }
  }

  async function handleArchive() {
    setErrorMessage(null)
    try {
      await archiveChannel.mutateAsync({ workspaceId, channelId: channel.id })
      await refreshChannelData()
      setIsArchiveConfirmOpen(false)
    } catch (error) {
      setErrorMessage(toErrorMessage(error, '채널을 보관하지 못했습니다.'))
    }
  }

  async function handleRestore() {
    setErrorMessage(null)
    try {
      await restoreChannel.mutateAsync({ workspaceId, channelId: channel.id })
      await refreshChannelData()
    } catch (error) {
      setErrorMessage(toErrorMessage(error, '채널을 복원하지 못했습니다.'))
    }
  }

  return (
    <div className={styles.layer}>
      <button
        className={styles.overlay}
        type="button"
        aria-label="채널 설정 닫기"
        onClick={onClose}
        disabled={isSaving}
      />
      <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="channel-settings-title">
        <header className={styles.header}>
          <div>
            <p>채널 설정</p>
            <h2 id="channel-settings-title">{channel.name}</h2>
          </div>
          <button type="button" onClick={onClose} disabled={isSaving}>닫기</button>
        </header>
        {isArchived ? (
          <div className={styles.content}>
            <section className={styles.archivedPanel} aria-labelledby="channel-archived-title">
              <strong id="channel-archived-title">보관된 채널입니다.</strong>
              <p>
                {channel.archiveExpiresAt
                  ? `${new Date(channel.archiveExpiresAt).toLocaleDateString('ko-KR')}에 자동 삭제됩니다.`
                  : '복원 가능 기간 안에만 채널을 복원할 수 있습니다.'}
              </p>
              <button type="button" onClick={() => void handleRestore()} disabled={isSaving}>
                {restoreChannel.isPending ? '복원 중...' : '채널 복원'}
              </button>
            </section>
            {errorMessage && <p className={styles.error} role="alert">{errorMessage}</p>}
          </div>
        ) : (
          <form className={styles.content} onSubmit={(event) => void handleSubmit(event)}>
            <label className={styles.field}>
              <span>채널 이름</span>
              <input
                value={name}
                maxLength={80}
                onChange={(event) => {
                  setName(event.target.value)
                  setErrorMessage(null)
                }}
              />
            </label>
            <label className={styles.field}>
              <span>설명</span>
              <textarea
                value={description}
                maxLength={300}
                rows={4}
                onChange={(event) => {
                  setDescription(event.target.value)
                  setErrorMessage(null)
                }}
              />
            </label>
            {errorMessage && <p className={styles.error} role="alert">{errorMessage}</p>}
            <section className={styles.archiveSection} aria-labelledby="channel-archive-title">
              <div>
                <strong id="channel-archive-title">채널 보관</strong>
                <p>메시지를 읽기 전용으로 전환하며, 15일 안에만 복원할 수 있습니다.</p>
              </div>
              <button type="button" onClick={() => setIsArchiveConfirmOpen(true)} disabled={isSaving}>
                채널 보관
              </button>
            </section>
            <footer className={styles.footer}>
              <button type="button" onClick={onClose} disabled={isSaving}>취소</button>
              <button type="submit" disabled={isSaving || isUnchanged}>
                {updateChannel.isPending ? '저장 중...' : '변경 사항 저장'}
              </button>
            </footer>
          </form>
        )}
      </section>
      {isArchiveConfirmOpen && (
        <div className={styles.confirmLayer}>
          <section className={styles.confirmDialog} role="alertdialog" aria-modal="true" aria-labelledby="channel-archive-confirm-title">
            <h2 id="channel-archive-confirm-title">채널을 보관할까요?</h2>
            <p>채널은 읽기 전용으로 전환되며 15일 안에만 복원할 수 있습니다.</p>
            <div>
              <button type="button" onClick={() => setIsArchiveConfirmOpen(false)} disabled={isSaving}>취소</button>
              <button type="button" onClick={() => void handleArchive()} disabled={isSaving}>
                {archiveChannel.isPending ? '보관 중...' : '보관'}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
