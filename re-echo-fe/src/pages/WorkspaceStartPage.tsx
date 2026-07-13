import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useCreateWorkspace } from '../features/workspace/useCreateWorkspace'
import {
  useWorkspaceList,
  workspaceListQueryKey,
} from '../features/workspace/useWorkspaceList'
import { ApiError } from '../shared/api/apiTypes'
import styles from './WorkspaceStartPage.module.css'

interface ValidationErrorResult {
  fieldErrors?: Array<{
    field: string
    reason: string
  }>
}

function getFieldError(error: unknown, field: string) {
  if (!(error instanceof ApiError)) {
    return null
  }

  const result = error.result as ValidationErrorResult | null
  return result?.fieldErrors?.find((fieldError) => fieldError.field === field)
    ?.reason ?? null
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

export function WorkspaceStartPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createWorkspace = useCreateWorkspace()
  const workspaceListQuery = useWorkspaceList()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [inviteCode, setInviteCode] = useState('')
  const [inviteCodeError, setInviteCodeError] = useState<string | null>(null)
  const [clientError, setClientError] = useState<string | null>(null)
  const trimmedName = name.trim()
  const trimmedDescription = description.trim()
  const nameError = clientError ?? getFieldError(createWorkspace.error, 'name')
  const workspaces = workspaceListQuery.data?.contents ?? []
  const ownedWorkspaces = workspaces.filter((workspace) => workspace.role === 'OWNER')
  const joinedWorkspaces = workspaces.filter((workspace) => workspace.role !== 'OWNER')

  async function handleCreateWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClientError(null)
    createWorkspace.reset()

    if (!trimmedName) {
      setClientError('워크스페이스 이름을 입력해 주세요.')
      return
    }

    if (trimmedName.length > 100) {
      setClientError('워크스페이스 이름은 100자 이하로 입력해 주세요.')
      return
    }

    try {
      const createdWorkspace = await createWorkspace.mutateAsync({
        name: trimmedName,
        description: trimmedDescription || null,
      })
      await queryClient.invalidateQueries({ queryKey: workspaceListQueryKey })
      void navigate(`/workspaces/${createdWorkspace.id}`)
    } catch {
      // mutation 상태를 통해 오류 메시지를 화면에 표시한다.
    }
  }

  function handleInviteSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setInviteCodeError(null)

    const inviteToken = getInviteToken(inviteCode)
    if (!inviteToken) {
      setInviteCodeError('초대 코드 또는 초대 링크를 입력해 주세요.')
      return
    }

    void navigate(`/invite-links/${encodeURIComponent(inviteToken)}`)
  }

  return (
    <AppShell>
      <section className={styles.page} aria-labelledby="workspace-start-title">
        <div className={styles.heading}>
          <p>Re-Echo 홈</p>
          <h1 id="workspace-start-title">내 워크스페이스</h1>
          <span>
            워크스페이스를 선택하거나 새 팀을 만들고 초대 링크로 참여하세요.
          </span>
        </div>

        {workspaceListQuery.isLoading && (
          <section className={styles.workspaceSection} aria-label="워크스페이스를 불러오는 중">
            <div className={styles.workspaceSectionHeader}>
              <p>워크스페이스를 불러오는 중입니다.</p>
            </div>
            <div className={styles.workspaceSkeletonGrid}>
              <span />
              <span />
            </div>
          </section>
        )}

        {workspaceListQuery.isError && (
          <section className={styles.workspaceSection}>
            <p className={styles.workspaceError}>
              워크스페이스 목록을 불러올 수 없습니다.
            </p>
            <button
              type="button"
              className={styles.workspaceRetryButton}
              onClick={() => void workspaceListQuery.refetch()}
            >
              다시 시도
            </button>
          </section>
        )}

        {!workspaceListQuery.isLoading &&
          !workspaceListQuery.isError &&
          workspaces.length > 0 && (
            <div className={styles.workspaceSections}>
              {ownedWorkspaces.length > 0 && (
                <section className={styles.workspaceSection} aria-labelledby="owned-workspace-title">
                  <div className={styles.workspaceSectionHeader}>
                    <p id="owned-workspace-title">내가 만든 워크스페이스</p>
                    <span>{ownedWorkspaces.length}</span>
                  </div>
                  <div className={styles.workspaceGrid}>
                    {ownedWorkspaces.map((workspace) => (
                      <Link
                        key={workspace.id}
                        className={styles.workspaceCard}
                        to={`/workspaces/${workspace.id}`}
                      >
                        <strong>{workspace.name}</strong>
                        <span>워크스페이스 홈으로 이동</span>
                        <small>읽지 않은 채널 {workspace.unreadChannelCount}개</small>
                      </Link>
                    ))}
                  </div>
                </section>
              )}

              {joinedWorkspaces.length > 0 && (
                <section className={styles.workspaceSection} aria-labelledby="joined-workspace-title">
                  <div className={styles.workspaceSectionHeader}>
                    <p id="joined-workspace-title">참여 중인 워크스페이스</p>
                    <span>{joinedWorkspaces.length}</span>
                  </div>
                  <div className={styles.workspaceGrid}>
                    {joinedWorkspaces.map((workspace) => (
                      <Link
                        key={workspace.id}
                        className={styles.workspaceCard}
                        to={`/workspaces/${workspace.id}`}
                      >
                        <strong>{workspace.name}</strong>
                        <span>워크스페이스 홈으로 이동</span>
                        <small>읽지 않은 채널 {workspace.unreadChannelCount}개</small>
                      </Link>
                    ))}
                  </div>
                </section>
              )}
            </div>
          )}

        <div className={styles.options}>
          <article className={styles.card}>
            <span className={styles.icon} aria-hidden="true">
              +
            </span>
            <h2>새 워크스페이스 만들기</h2>
            <p>팀 이름을 정하고 기본 채널에서 대화를 시작합니다.</p>
            <form
              className={styles.createForm}
              onSubmit={(event) => void handleCreateWorkspace(event)}
            >
              <div className={styles.field}>
                <label htmlFor="workspace-name">워크스페이스 이름</label>
                <input
                  id="workspace-name"
                  name="name"
                  type="text"
                  value={name}
                  maxLength={100}
                  aria-describedby={nameError ? 'workspace-name-error' : undefined}
                  aria-invalid={Boolean(nameError)}
                  onChange={(event) => {
                    setName(event.target.value)
                    setClientError(null)
                    createWorkspace.reset()
                  }}
                />
                {nameError && (
                  <small id="workspace-name-error" className={styles.fieldError} role="alert">
                    {nameError}
                  </small>
                )}
              </div>

              <div className={styles.field}>
                <label htmlFor="workspace-description">설명</label>
                <textarea
                  id="workspace-description"
                  name="description"
                  value={description}
                  maxLength={500}
                  rows={3}
                  onChange={(event) => {
                    setDescription(event.target.value)
                    createWorkspace.reset()
                  }}
                />
              </div>

              {createWorkspace.isError && !nameError && (
                <small className={styles.formError} role="alert">
                  {createWorkspace.error instanceof ApiError
                    ? createWorkspace.error.message
                    : '워크스페이스를 만들지 못했습니다.'}
                </small>
              )}

              {createWorkspace.isSuccess && (
                <small className={styles.success} role="status">
                  워크스페이스를 만들었습니다.
                </small>
              )}

              <button type="submit" disabled={createWorkspace.isPending}>
                {createWorkspace.isPending ? '만드는 중...' : '워크스페이스 만들기'}
              </button>
            </form>
          </article>

          <article className={styles.card}>
            <span className={styles.icon} aria-hidden="true">
              ↗
            </span>
            <h2>초대 링크로 참여</h2>
            <p>팀에서 전달받은 초대 링크를 확인하고 참여합니다.</p>
            <form className={styles.createForm} onSubmit={handleInviteSubmit}>
              <div className={styles.field}>
                <label htmlFor="invite-code">초대 코드 또는 링크</label>
                <input
                  id="invite-code"
                  name="inviteCode"
                  type="text"
                  value={inviteCode}
                  aria-describedby={
                    inviteCodeError ? 'invite-code-error' : 'invite-code-help'
                  }
                  aria-invalid={Boolean(inviteCodeError)}
                  onChange={(event) => {
                    setInviteCode(event.target.value)
                    setInviteCodeError(null)
                  }}
                />
                {inviteCodeError ? (
                  <small id="invite-code-error" className={styles.fieldError} role="alert">
                    {inviteCodeError}
                  </small>
                ) : (
                  <small id="invite-code-help">
                    복사한 초대 링크 전체를 붙여넣어도 됩니다.
                  </small>
                )}
              </div>
              <button type="submit">초대 확인하기</button>
            </form>
          </article>
        </div>
      </section>
    </AppShell>
  )
}
