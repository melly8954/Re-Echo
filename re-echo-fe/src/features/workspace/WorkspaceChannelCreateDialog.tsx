import { useEffect, useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useCreateWorkspaceChannel } from './useCreateWorkspaceChannel'
import { useWorkspaceMembers } from './useWorkspaceMembers'
import { workspaceChannelsQueryKey } from './useWorkspaceChannels'
import { getWorkspaceRoleLabel } from './workspaceLabels'
import type { ChannelVisibility } from './workspaceApi'
import { ApiError } from '../../shared/api/apiTypes'
import styles from './WorkspaceChannelCreateDialog.module.css'

interface WorkspaceChannelCreateDialogProps {
  workspaceId: string
  isOpen: boolean
  onClose: () => void
  onCreated: (channelId: string) => void
}

// 워크스페이스 홈에서 채널을 생성하고 생성된 채널로 이동시킨다.
export function WorkspaceChannelCreateDialog({
  workspaceId,
  isOpen,
  onClose,
  onCreated,
}: WorkspaceChannelCreateDialogProps) {
  const queryClient = useQueryClient()
  const createChannel = useCreateWorkspaceChannel()
  const workspaceMembersQuery = useWorkspaceMembers(workspaceId)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [visibility, setVisibility] = useState<ChannelVisibility>('PUBLIC')
  const [memberIds, setMemberIds] = useState<string[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const members = workspaceMembersQuery.data?.contents ?? []

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !createChannel.isPending) {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [createChannel.isPending, isOpen, onClose])

  if (!isOpen) {
    return null
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedName = name.trim()
    if (!trimmedName) {
      setErrorMessage('채널 이름을 입력해 주세요.')
      return
    }

    setErrorMessage(null)
    try {
      const channel = await createChannel.mutateAsync({
        workspaceId,
        request: {
          name: trimmedName,
          description: description.trim() || null,
          visibility,
          memberIds: visibility === 'PRIVATE' ? memberIds : [],
        },
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelsQueryKey(workspaceId),
      })
      onCreated(channel.id)
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : '채널을 생성하지 못했습니다.')
    }
  }

  function toggleMember(memberId: string) {
    setMemberIds((currentMemberIds) =>
      currentMemberIds.includes(memberId)
        ? currentMemberIds.filter((currentMemberId) => currentMemberId !== memberId)
        : [...currentMemberIds, memberId],
    )
  }

  return (
    <div className={styles.layer}>
      <button className={styles.overlay} type="button" aria-label="채널 생성 닫기" onClick={onClose} />
      <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="channel-create-title">
        <header>
          <div>
            <p>새 채널</p>
            <h2 id="channel-create-title">채널 생성</h2>
          </div>
          <button type="button" onClick={onClose} disabled={createChannel.isPending}>닫기</button>
        </header>
        <form onSubmit={(event) => void handleSubmit(event)}>
          <label>
            <span>채널 이름</span>
            <input value={name} maxLength={80} autoFocus onChange={(event) => { setName(event.target.value); setErrorMessage(null) }} />
          </label>
          <label>
            <span>채널 설명 (선택)</span>
            <textarea
              value={description}
              maxLength={300}
              rows={3}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>
          <label>
            <span>공개 범위</span>
            <select value={visibility} onChange={(event) => { const nextVisibility = event.target.value as ChannelVisibility; setVisibility(nextVisibility); if (nextVisibility === 'PUBLIC') setMemberIds([]) }}>
              <option value="PUBLIC">공개</option>
              <option value="PRIVATE">비공개</option>
            </select>
          </label>
          {visibility === 'PRIVATE' && (
            <fieldset>
              <legend>초기 멤버</legend>
              {workspaceMembersQuery.isLoading && <p>멤버 목록을 불러오는 중입니다.</p>}
              {!workspaceMembersQuery.isLoading && members.map((member) => (
                <label key={member.id} className={styles.member}>
                  <input type="checkbox" checked={memberIds.includes(member.id)} onChange={() => toggleMember(member.id)} />
                  <span>{member.displayName}</span>
                  <small>{getWorkspaceRoleLabel(member.role)}</small>
                </label>
              ))}
            </fieldset>
          )}
          {errorMessage && <p className={styles.error} role="alert">{errorMessage}</p>}
          <footer>
            <button type="button" onClick={onClose} disabled={createChannel.isPending}>취소</button>
            <button type="submit" disabled={createChannel.isPending}>{createChannel.isPending ? '생성 중' : '채널 만들기'}</button>
          </footer>
        </form>
      </section>
    </div>
  )
}
