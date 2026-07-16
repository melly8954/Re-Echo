import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { ChannelMessagePanel } from '../features/message/ChannelMessagePanel'
import { WorkspaceChannelSettingsDialog } from '../features/workspace/WorkspaceChannelSettingsDialog'
import { WorkspaceProfileDialog } from '../features/workspace/WorkspaceProfileDialog'
import { useAddWorkspacePrivateChannelMembers } from '../features/workspace/useAddWorkspacePrivateChannelMembers'
import { useCreateWorkspaceChannel } from '../features/workspace/useCreateWorkspaceChannel'
import { useJoinWorkspaceChannel } from '../features/workspace/useJoinWorkspaceChannel'
import { useLeaveWorkspaceChannel } from '../features/workspace/useLeaveWorkspaceChannel'
import {
  useWorkspaceChannelMembers,
  workspaceChannelMembersQueryKey,
} from '../features/workspace/useWorkspaceChannelMembers'
import {
  useWorkspaceChannels,
  workspaceChannelsQueryKey,
} from '../features/workspace/useWorkspaceChannels'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import { useWorkspaceChannelDetail } from '../features/workspace/useWorkspaceChannelDetail'
import {
  getWorkspaceMembershipStatusLabel,
  getWorkspaceRoleLabel,
} from '../features/workspace/workspaceLabels'
import { useWorkspaceMembers } from '../features/workspace/useWorkspaceMembers'
import type {
  ChannelVisibility,
  WorkspaceChannel,
  WorkspaceMembershipRole,
} from '../features/workspace/workspaceApi'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspacePage.module.css'

// 워크스페이스와 채널 경로 파라미터를 실제 화면 상태로 연결한다.
export function WorkspacePage() {
  const { workspaceId, channelId } = useParams()
  const routeWorkspaceId = workspaceId ?? ''
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createChannel = useCreateWorkspaceChannel()
  const addPrivateChannelMembers = useAddWorkspacePrivateChannelMembers()
  const joinChannel = useJoinWorkspaceChannel()
  const leaveChannel = useLeaveWorkspaceChannel()
  const workspaceQuery = useWorkspaceDetail(routeWorkspaceId)
  const channelsQuery = useWorkspaceChannels(routeWorkspaceId)
  const workspaceMembersQuery = useWorkspaceMembers(routeWorkspaceId)
  const [channelName, setChannelName] = useState('')
  const [channelDescription, setChannelDescription] = useState('')
  const [channelVisibility, setChannelVisibility] =
    useState<ChannelVisibility>('PUBLIC')
  const [channelCreateError, setChannelCreateError] = useState<string | null>(null)
  const [selectedPrivateMemberIds, setSelectedPrivateMemberIds] = useState<
    string[]
  >([])
  const [selectedChannelMemberIds, setSelectedChannelMemberIds] = useState<
    string[]
  >([])
  const [channelMemberAddError, setChannelMemberAddError] = useState<
    string | null
  >(null)
  const [channelMembershipMessage, setChannelMembershipMessage] = useState<
    string | null
  >(null)
  const [isChannelCreateOpen, setIsChannelCreateOpen] = useState(false)
  const [isChannelMemberAddOpen, setIsChannelMemberAddOpen] = useState(false)
  const [isChannelSettingsOpen, setIsChannelSettingsOpen] = useState(false)
  const [isWorkspaceProfileOpen, setIsWorkspaceProfileOpen] = useState(false)
  const [isChannelLeaveConfirmOpen, setIsChannelLeaveConfirmOpen] = useState(false)
  const [channelSettingsTargetId, setChannelSettingsTargetId] = useState<string | null>(null)
  const [channelLeaveTargetId, setChannelLeaveTargetId] = useState<string | null>(null)
  const [channelLeaveError, setChannelLeaveError] = useState<string | null>(null)

  useEffect(() => {
    if (!isChannelCreateOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Escape') {
        return
      }
      if (isChannelCreateOpen && !createChannel.isPending) {
        setIsChannelCreateOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [
    createChannel.isPending,
    isChannelCreateOpen,
  ])

  const workspace = workspaceQuery.data
  const channels = channelsQuery.data?.contents ?? []
  const activeChannel = channels.find((channel) => channel.id === channelId)
  const channelDetailQuery = useWorkspaceChannelDetail(
    routeWorkspaceId,
    activeChannel?.id ?? '',
  )
  const activeChannelDetail = channelDetailQuery.data
  const channelSettingsDetailQuery = useWorkspaceChannelDetail(
    routeWorkspaceId,
    channelSettingsTargetId ?? '',
  )
  const settingsChannelDetail =
    channelSettingsTargetId === activeChannel?.id
      ? activeChannelDetail
      : channelSettingsDetailQuery.data
  const channelMembersQuery = useWorkspaceChannelMembers(
    routeWorkspaceId,
    activeChannel?.id ?? '',
  )

  useEffect(() => {
    setChannelMembershipMessage(null)
    setChannelMemberAddError(null)
    setSelectedChannelMemberIds([])
    setIsChannelMemberAddOpen(false)
    setIsChannelSettingsOpen(false)
    setIsChannelLeaveConfirmOpen(false)
    setChannelSettingsTargetId(null)
    setChannelLeaveTargetId(null)
    setChannelLeaveError(null)
  }, [activeChannel?.id])

  useEffect(() => {
    if (!isChannelLeaveConfirmOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !leaveChannel.isPending) {
        setIsChannelLeaveConfirmOpen(false)
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [isChannelLeaveConfirmOpen, leaveChannel.isPending])

  if (!workspaceId) {
    return <Navigate to="/" replace />
  }
  if (workspace?.status === 'ARCHIVED') {
    return <Navigate to={`/workspaces/${workspaceId}`} replace />
  }
  const canIssueInvite =
    workspace?.myMembership.role === 'OWNER' ||
    workspace?.myMembership.role === 'ADMIN'
  const canCreateChannel = canIssueInvite
  const canManageActivePrivateChannel =
    canCreateChannel &&
    activeChannel?.visibility === 'PRIVATE' &&
    activeChannel.joined
  const leavingChannel = channels.find((channel) => channel.id === channelLeaveTargetId)
  const isChannelMembershipPending = joinChannel.isPending || leaveChannel.isPending
  const members = channelMembersQuery.data?.contents ?? []
  const privateChannelMemberOptions =
    workspaceMembersQuery.data?.contents.filter(
      (member) => member.id !== workspace?.myMembership.id,
    ) ?? []
  const activeChannelMemberIds = new Set(
    channelMembersQuery.data?.contents.map((member) => member.id) ?? [],
  )
  const privateChannelAddOptions =
    workspaceMembersQuery.data?.contents.filter(
      (member) => !activeChannelMemberIds.has(member.id),
    ) ?? []
  const isChannelMemberAddLoading =
    workspaceMembersQuery.isLoading || channelMembersQuery.isLoading
  const isChannelMemberAddUnavailable =
    workspaceMembersQuery.isError || channelMembersQuery.isError
  const memberGroups: Array<{
    role: WorkspaceMembershipRole
    label: string
    members: typeof members
  }> = [
    {
      role: 'OWNER',
      label: '소유자',
      members: members.filter((member) => member.role === 'OWNER'),
    },
    {
      role: 'ADMIN',
      label: '관리자',
      members: members.filter((member) => member.role === 'ADMIN'),
    },
    {
      role: 'MEMBER',
      label: '멤버',
      members: members.filter((member) => member.role === 'MEMBER'),
    },
  ]

  function canLeaveChannel(channel: WorkspaceChannel) {
    return channel.joined &&
      !channel.isGeneral &&
      !(channel.visibility === 'PRIVATE' && channel.createdByMe)
  }

  async function handleCreateChannel(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!workspaceId || !canCreateChannel) {
      return
    }

    const trimmedName = channelName.trim()
    if (!trimmedName) {
      setChannelCreateError('채널 이름을 입력해 주세요.')
      return
    }

    setChannelCreateError(null)
    try {
      const createdChannel = await createChannel.mutateAsync({
        workspaceId,
        request: {
          name: trimmedName,
          description: channelDescription.trim() || null,
          visibility: channelVisibility,
          memberIds:
            channelVisibility === 'PRIVATE' ? selectedPrivateMemberIds : [],
        },
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelsQueryKey(workspaceId),
      })
      setChannelName('')
      setChannelDescription('')
      setChannelVisibility('PUBLIC')
      setSelectedPrivateMemberIds([])
      setIsChannelCreateOpen(false)
      void navigate(`/workspaces/${workspaceId}/channels/${createdChannel.id}`)
    } catch (error) {
      if (error instanceof ApiError) {
        setChannelCreateError(error.message)
        return
      }
      setChannelCreateError('채널을 생성하지 못했습니다.')
    }
  }

  async function handleJoinActiveChannel() {
    if (!workspaceId || !activeChannel || activeChannel.joined) {
      return
    }

    setChannelMembershipMessage(null)
    try {
      await joinChannel.mutateAsync({
        workspaceId,
        channelId: activeChannel.id,
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelsQueryKey(workspaceId),
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelMembersQueryKey(workspaceId, activeChannel.id),
      })
      setChannelMembershipMessage('채널에 참여했습니다.')
    } catch (error) {
      if (error instanceof ApiError) {
        setChannelMembershipMessage(error.message)
        return
      }
      setChannelMembershipMessage('채널에 참여하지 못했습니다.')
    }
  }

  async function handleLeaveChannel() {
    if (!workspaceId || !leavingChannel || !canLeaveChannel(leavingChannel)) {
      return
    }

    setChannelLeaveError(null)
    try {
      await leaveChannel.mutateAsync({
        workspaceId,
        channelId: leavingChannel.id,
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelsQueryKey(workspaceId),
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelMembersQueryKey(workspaceId, leavingChannel.id),
      })
      setIsChannelLeaveConfirmOpen(false)
      setChannelLeaveTargetId(null)
      if (activeChannel?.id === leavingChannel.id) {
        setChannelMembershipMessage('채널에서 나갔습니다.')
      }
    } catch (error) {
      if (error instanceof ApiError) {
        setChannelLeaveError(error.message)
        return
      }
      setChannelLeaveError('채널에서 나가지 못했습니다.')
    }
  }

  async function handleAddPrivateChannelMembers(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!workspaceId || !activeChannel || !canManageActivePrivateChannel) {
      return
    }
    if (selectedChannelMemberIds.length === 0) {
      setChannelMemberAddError('추가할 멤버를 선택해 주세요.')
      return
    }

    setChannelMemberAddError(null)
    try {
      await addPrivateChannelMembers.mutateAsync({
        workspaceId,
        channelId: activeChannel.id,
        request: {
          memberIds: selectedChannelMemberIds,
        },
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelMembersQueryKey(workspaceId, activeChannel.id),
      })
      await queryClient.invalidateQueries({
        queryKey: workspaceChannelsQueryKey(workspaceId),
      })
      setSelectedChannelMemberIds([])
      setIsChannelMemberAddOpen(false)
      setChannelMembershipMessage('비공개 채널 멤버를 추가했습니다.')
    } catch (error) {
      if (error instanceof ApiError) {
        setChannelMemberAddError(error.message)
        return
      }
      setChannelMemberAddError('비공개 채널 멤버를 추가하지 못했습니다.')
    }
  }

  function closeChannelCreateDialog() {
    if (createChannel.isPending) {
      return
    }
    setIsChannelCreateOpen(false)
  }

  function openChannelSettings(channelId: string) {
    const channel = channels.find((item) => item.id === channelId)
    if (!canCreateChannel || !channel || channel.isGeneral) {
      return
    }
    setChannelSettingsTargetId(channel.id)
    setIsChannelSettingsOpen(true)
  }

  function requestLeaveChannel(channelId: string) {
    const channel = channels.find((item) => item.id === channelId)
    if (!channel || !canLeaveChannel(channel)) {
      return
    }
    leaveChannel.reset()
    setChannelLeaveError(null)
    setChannelLeaveTargetId(channel.id)
    setIsChannelLeaveConfirmOpen(true)
  }

  function togglePrivateChannelMember(memberId: string) {
    setSelectedPrivateMemberIds((currentMemberIds) =>
      currentMemberIds.includes(memberId)
        ? currentMemberIds.filter((currentMemberId) => currentMemberId !== memberId)
        : [...currentMemberIds, memberId],
    )
  }

  function toggleChannelMemberToAdd(memberId: string) {
    setSelectedChannelMemberIds((currentMemberIds) =>
      currentMemberIds.includes(memberId)
        ? currentMemberIds.filter((currentMemberId) => currentMemberId !== memberId)
        : [...currentMemberIds, memberId],
    )
    setChannelMemberAddError(null)
  }

  const channelCreateAction = canCreateChannel ? (
    <button
      className={styles.channelCreateActionButton}
      type="button"
      aria-label="채널 생성"
      title="채널 생성"
      onClick={() => {
        setChannelCreateError(null)
        setIsChannelCreateOpen(true)
      }}
    >
      +
    </button>
  ) : undefined

  const channelCreateDialog = canCreateChannel && isChannelCreateOpen && (
    <div className={styles.channelCreateLayer}>
      <button
        className={styles.channelCreateOverlay}
        type="button"
        aria-label="채널 생성 닫기"
        onClick={closeChannelCreateDialog}
      />
      <section
        className={styles.channelCreateDialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="channel-create-title"
      >
        <header>
          <div>
            <p className={styles.eyebrow}>새 채널</p>
            <h2 id="channel-create-title">채널 생성</h2>
          </div>
          <button
            className={styles.channelCreateCloseButton}
            type="button"
            onClick={closeChannelCreateDialog}
          >
            닫기
          </button>
        </header>

        <form className={styles.channelCreateForm} onSubmit={handleCreateChannel}>
          <label>
            <span className={styles.channelCreateFieldLabel}>채널 이름</span>
            <input
              type="text"
              value={channelName}
              maxLength={80}
              placeholder="예: design"
              autoFocus
              onChange={(event) => {
                setChannelName(event.target.value)
                setChannelCreateError(null)
              }}
            />
          </label>
          <label>
            <span className={styles.channelCreateFieldLabel}>채널 설명 (선택)</span>
            <textarea
              value={channelDescription}
              maxLength={300}
              rows={3}
              placeholder="예: 디자인 논의와 피드백을 위한 채널"
              onChange={(event) => setChannelDescription(event.target.value)}
            />
          </label>
          <label>
            <span className={styles.channelCreateFieldLabel}>공개 범위</span>
            <select
              value={channelVisibility}
              onChange={(event) => {
                const nextVisibility = event.target.value as ChannelVisibility
                setChannelVisibility(nextVisibility)
                if (nextVisibility === 'PUBLIC') {
                  setSelectedPrivateMemberIds([])
                }
              }}
            >
              <option value="PUBLIC">공개</option>
              <option value="PRIVATE">비공개</option>
            </select>
          </label>
          {channelVisibility === 'PRIVATE' && (
            <fieldset className={styles.channelCreateMemberFieldset}>
              <legend>
                초기 멤버
                <span>{selectedPrivateMemberIds.length}</span>
              </legend>
              {workspaceMembersQuery.isLoading && (
                <div className={styles.channelCreateMemberSkeleton} aria-label="멤버 목록을 불러오는 중">
                  <span />
                  <span />
                  <span />
                </div>
              )}
              {workspaceMembersQuery.isError && (
                <p className={styles.channelCreateError}>
                  워크스페이스 멤버 목록을 불러올 수 없습니다.
                </p>
              )}
              {!workspaceMembersQuery.isLoading &&
                !workspaceMembersQuery.isError &&
                privateChannelMemberOptions.length === 0 && (
                  <p className={styles.channelCreateEmptyText}>
                    추가할 멤버가 없습니다.
                  </p>
                )}
              {!workspaceMembersQuery.isLoading &&
                !workspaceMembersQuery.isError &&
                privateChannelMemberOptions.length > 0 && (
                  <div className={styles.channelCreateMemberList}>
                    {privateChannelMemberOptions.map((member) => (
                      <label key={member.id} className={styles.channelCreateMemberItem}>
                        <input
                          type="checkbox"
                          checked={selectedPrivateMemberIds.includes(member.id)}
                          onChange={() => togglePrivateChannelMember(member.id)}
                        />
                        {member.profileImageUrl ? (
                          <img src={member.profileImageUrl} alt="" />
                        ) : (
                          <span
                            className={styles.channelCreateMemberAvatarFallback}
                            aria-hidden="true"
                          >
                            {member.displayName.slice(0, 1)}
                          </span>
                        )}
                        <span className={styles.channelCreateMemberName}>
                          {member.displayName}
                        </span>
                        <small>{getWorkspaceRoleLabel(member.role)}</small>
                      </label>
                    ))}
                  </div>
                )}
            </fieldset>
          )}
          {channelCreateError && (
            <p className={styles.channelCreateError} role="alert">
              {channelCreateError}
            </p>
          )}
          <div className={styles.channelCreateFooter}>
            <button
              type="button"
              className={styles.channelCreateSecondaryButton}
              onClick={closeChannelCreateDialog}
            >
              취소
            </button>
            <button
              type="submit"
              className={styles.channelCreatePrimaryButton}
              disabled={createChannel.isPending || !channelName.trim()}
            >
              {createChannel.isPending ? '생성 중' : '생성'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )

  const memberPanel = (
    <section className={styles.memberPanel} aria-labelledby="workspace-member-title">
      <div className={styles.memberPanelHeader}>
        <div>
          <p className={styles.eyebrow}>참여자</p>
          <h2 id="workspace-member-title">채널 참여자</h2>
          <p className={styles.memberPanelContext}>
            {`${activeChannel?.visibility === 'PRIVATE' ? '잠금' : '#'} ${
              activeChannel?.name ?? '채널'
            }`}
          </p>
        </div>
        <span>{members.length}</span>
      </div>

      {canManageActivePrivateChannel && (
        <div className={styles.memberAddToolbar}>
          <p>비공개 채널 멤버는 관리자가 직접 추가합니다.</p>
          <button
            type="button"
            onClick={() => {
              setChannelMemberAddError(null)
              setIsChannelMemberAddOpen((isOpen) => !isOpen)
            }}
          >
            {isChannelMemberAddOpen ? '닫기' : '멤버 추가'}
          </button>
        </div>
      )}

      {canManageActivePrivateChannel && isChannelMemberAddOpen && (
          <form
            className={styles.memberAddPanel}
            onSubmit={handleAddPrivateChannelMembers}
          >
            <div className={styles.memberAddPanelHeader}>
              <strong>추가할 멤버</strong>
              <span>{selectedChannelMemberIds.length}</span>
            </div>
            {isChannelMemberAddLoading && (
              <div className={styles.memberSkeletonList} aria-label="추가 가능 멤버를 불러오는 중">
                <span />
                <span />
              </div>
            )}
            {isChannelMemberAddUnavailable && (
              <p className={styles.channelCreateError}>
                추가 가능 멤버를 불러올 수 없습니다.
              </p>
            )}
            {!isChannelMemberAddLoading &&
              !isChannelMemberAddUnavailable &&
              privateChannelAddOptions.length === 0 && (
                <p className={styles.channelCreateEmptyText}>
                  추가할 워크스페이스 멤버가 없습니다.
                </p>
              )}
            {!isChannelMemberAddLoading &&
              !isChannelMemberAddUnavailable &&
              privateChannelAddOptions.length > 0 && (
                <div className={styles.memberAddList}>
                  {privateChannelAddOptions.map((member) => (
                    <label key={member.id} className={styles.memberAddItem}>
                      <input
                        type="checkbox"
                        checked={selectedChannelMemberIds.includes(member.id)}
                        onChange={() => toggleChannelMemberToAdd(member.id)}
                      />
                      {member.profileImageUrl ? (
                        <img src={member.profileImageUrl} alt="" />
                      ) : (
                        <span className={styles.memberAvatarFallback} aria-hidden="true">
                          {member.displayName.slice(0, 1)}
                        </span>
                      )}
                      <span>{member.displayName}</span>
                      <small>{getWorkspaceRoleLabel(member.role)}</small>
                    </label>
                  ))}
                </div>
              )}
            {channelMemberAddError && (
              <p className={styles.channelCreateError} role="alert">
                {channelMemberAddError}
              </p>
            )}
            <button
              type="submit"
              disabled={
                addPrivateChannelMembers.isPending ||
                selectedChannelMemberIds.length === 0
              }
            >
              {addPrivateChannelMembers.isPending ? '추가 중' : '추가'}
            </button>
          </form>
        )}

      {channelMembersQuery.isLoading && (
        <div className={styles.memberSkeletonList} aria-label="참여자 목록을 불러오는 중">
          <span />
          <span />
          <span />
        </div>
      )}

      {channelMembersQuery.isError && (
        <div className={styles.memberError}>
          <p>채널 참여자 목록을 불러올 수 없습니다.</p>
          <button type="button" onClick={() => void channelMembersQuery.refetch()}>
            다시 시도
          </button>
        </div>
      )}

      {!channelMembersQuery.isLoading && !channelMembersQuery.isError && (
        <div className={styles.memberGroups}>
          {memberGroups
            .filter((group) => group.members.length > 0)
            .map((group) => (
              <section key={group.role} className={styles.memberGroup}>
                <h3>
                  {group.label}
                  <span>{group.members.length}</span>
                </h3>
                <div className={styles.memberList}>
                  {group.members.map((member) => {
                    const isCurrentMember = member.id === workspace?.myMembership.id

                    return (
                      <div
                        key={member.id}
                        className={
                          isCurrentMember
                            ? `${styles.memberItem} ${styles.memberItemSelf}`
                            : styles.memberItem
                        }
                        role={isCurrentMember ? 'button' : undefined}
                        tabIndex={isCurrentMember ? 0 : undefined}
                        aria-label={isCurrentMember ? '내 워크스페이스 프로필 편집' : undefined}
                        title={isCurrentMember ? '우클릭하여 워크스페이스 프로필 편집' : undefined}
                        onContextMenu={isCurrentMember ? (event) => {
                          event.preventDefault()
                          setIsWorkspaceProfileOpen(true)
                        } : undefined}
                        onKeyDown={isCurrentMember ? (event) => {
                          if (
                            event.key === 'Enter' ||
                            event.key === ' ' ||
                            event.key === 'ContextMenu' ||
                            (event.key === 'F10' && event.shiftKey)
                          ) {
                            event.preventDefault()
                            setIsWorkspaceProfileOpen(true)
                          }
                        } : undefined}
                      >
                        {member.profileImageUrl ? (
                          <img src={member.profileImageUrl} alt="" />
                        ) : (
                          <span className={styles.memberAvatarFallback} aria-hidden="true">
                            {member.displayName.slice(0, 1)}
                          </span>
                        )}
                        <div className={styles.memberContent}>
                          <div>
                            <strong>{member.displayName}</strong>
                            {isCurrentMember && <em>나</em>}
                          </div>
                          <small>{getWorkspaceMembershipStatusLabel(member.status)}</small>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </section>
            ))}
        </div>
      )}
    </section>
  )

  return (
    <AppShell
      workspaceId={workspaceId}
      workspaceName={workspace?.name}
      channels={channels.map((channel) => ({
        ...channel,
        href: `/workspaces/${workspaceId}/channels/${channel.id}`,
        canManage: canCreateChannel && !channel.isGeneral,
        canLeave: canLeaveChannel(channel),
      }))}
      activeChannelId={activeChannel?.id}
      isChannelsLoading={channelsQuery.isLoading}
      channelHeaderAction={channelCreateAction}
      onOpenChannelSettings={openChannelSettings}
      onRequestLeaveChannel={requestLeaveChannel}
      isChannelLeavePending={leaveChannel.isPending}
      onOpenWorkspaceProfile={workspace ? () => setIsWorkspaceProfileOpen(true) : undefined}
      rightSidebar={workspace ? memberPanel : undefined}
      rightSidebarLabel="채널 참여자"
    >
      <section className={styles.page} aria-labelledby="workspace-page-title">
        {workspaceQuery.isLoading && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>불러오는 중</p>
            <h1 id="workspace-page-title">워크스페이스를 여는 중입니다</h1>
          </div>
        )}

        {workspaceQuery.isError && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>진입 실패</p>
            <h1 id="workspace-page-title">워크스페이스를 열 수 없습니다</h1>
            <p className={styles.description}>
              {workspaceQuery.error instanceof ApiError
                ? workspaceQuery.error.message
                : '잠시 후 다시 시도해 주세요.'}
            </p>
            <button type="button" onClick={() => void workspaceQuery.refetch()}>
              다시 시도
            </button>
          </div>
        )}

        {channelsQuery.isError && workspace && (
          <div className={styles.panel}>
            <p className={styles.eyebrow}>채널 목록 오류</p>
            <h1 id="workspace-page-title">채널 목록을 불러올 수 없습니다</h1>
            <p className={styles.description}>
              {channelsQuery.error instanceof ApiError
                ? channelsQuery.error.message
                : '잠시 후 다시 시도해 주세요.'}
            </p>
            <button type="button" onClick={() => void channelsQuery.refetch()}>
              다시 시도
            </button>
          </div>
        )}

        {workspace && !channelsQuery.isError && (
          <div className={styles.workspace}>
            <header className={styles.header}>
              <div>
                <p className={styles.eyebrow}>
                  채널
                </p>
                <h1 id="workspace-page-title">
                  {activeChannel?.visibility === 'PRIVATE' ? '잠금 ' : '#'}
                  {activeChannel?.name ?? '채널'}
                </h1>
                <p className={styles.description}>
                  {activeChannelDetail?.description ?? '채널 설명이 없습니다.'}
                </p>
              </div>
            </header>

            {channelMembershipMessage && (
              <p className={styles.channelMembershipMessage} role="status">
                {channelMembershipMessage}
              </p>
            )}

            <section className={styles.messagePanel} aria-label="메시지 영역">
              {activeChannel?.status === 'ARCHIVED' && !activeChannel.joined ? (
                <div className={styles.channelJoinPrompt}>
                  <strong>{activeChannel.name}</strong>
                  <p>보관된 채널입니다. 현재는 읽기 전용이며 새로 참여할 수 없습니다.</p>
                </div>
              ) : activeChannel && !activeChannel.joined ? (
                <div className={styles.channelJoinPrompt}>
                  <strong>{activeChannel.name}</strong>
                  <p>
                    공개 채널입니다. 참여하면 과거 메시지 전체를 볼 수 있습니다.
                  </p>
                  <button
                    type="button"
                    onClick={() => void handleJoinActiveChannel()}
                    disabled={isChannelMembershipPending}
                  >
                    {joinChannel.isPending ? '참여 중' : '채널 참여'}
                  </button>
                </div>
              ) : (
                activeChannel && (
                  <ChannelMessagePanel
                    workspaceId={workspaceId}
                    channelId={activeChannel.id}
                    currentMembershipId={workspace.myMembership.id}
                    canManageMessages={
                      workspace.myMembership.role === 'OWNER' ||
                      workspace.myMembership.role === 'ADMIN'
                    }
                    channelName={activeChannel.name}
                    readOnly={activeChannel.status === 'ARCHIVED'}
                  />
                )
              )}
            </section>
          </div>
        )}
      </section>
      {channelCreateDialog}
      {isChannelLeaveConfirmOpen && leavingChannel && (
        <div className={styles.channelLeaveLayer}>
          <button
            className={styles.channelLeaveOverlay}
            type="button"
            aria-label="채널 나가기 닫기"
            onClick={() => setIsChannelLeaveConfirmOpen(false)}
            disabled={leaveChannel.isPending}
          />
          <section
            className={styles.channelLeaveDialog}
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="channel-leave-title"
            aria-describedby="channel-leave-description"
          >
            <p className={styles.eyebrow}>채널 나가기</p>
            <h2 id="channel-leave-title">{leavingChannel.name} 채널에서 나갈까요?</h2>
            <p id="channel-leave-description">
              나가면 이 채널의 메시지를 더 이상 볼 수 없습니다.
            </p>
            {channelLeaveError && (
              <p className={styles.channelLeaveError} role="alert">
                {channelLeaveError}
              </p>
            )}
            <div className={styles.channelLeaveFooter}>
              <button
                type="button"
                onClick={() => setIsChannelLeaveConfirmOpen(false)}
                disabled={leaveChannel.isPending}
              >
                취소
              </button>
              <button
                type="button"
                onClick={() => void handleLeaveChannel()}
                disabled={leaveChannel.isPending}
              >
                {leaveChannel.isPending ? '나가는 중...' : '나가기'}
              </button>
            </div>
          </section>
        </div>
      )}
      {settingsChannelDetail && canCreateChannel && !settingsChannelDetail.isGeneral && (
        <WorkspaceChannelSettingsDialog
          workspaceId={workspaceId}
          channel={settingsChannelDetail}
          isOpen={isChannelSettingsOpen}
          onClose={() => {
            setIsChannelSettingsOpen(false)
            setChannelSettingsTargetId(null)
          }}
        />
      )}
      {workspace && (
        <WorkspaceProfileDialog
          workspace={workspace}
          isOpen={isWorkspaceProfileOpen}
          onClose={() => setIsWorkspaceProfileOpen(false)}
        />
      )}
    </AppShell>
  )
}
