import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useCreateWorkspace } from '../features/workspace/useCreateWorkspace'
import { useWorkspaceList, workspaceListQueryKey } from '../features/workspace/useWorkspaceList'
import type { WorkspaceMembershipRole } from '../features/workspace/workspaceApi'
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

function getWorkspaceRoleLabel(role: WorkspaceMembershipRole) {
  if (role === 'OWNER') {
    return '소유자'
  }
  if (role === 'ADMIN') {
    return '관리자'
  }
  return '멤버'
}

export function WorkspaceStartPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const createWorkspace = useCreateWorkspace()
  const workspaceListQuery = useWorkspaceList()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
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

        {workspaceListQuery.isLoading && (
          <section className={styles.workspaceList} aria-label="워크스페이스 목록을 불러오는 중">
            <div className={styles.workspaceSkeleton} />
            <div className={styles.workspaceSkeleton} />
          </section>
        )}

        {workspaceListQuery.isError && (
          <section className={styles.listError} aria-live="polite">
            <p>워크스페이스 목록을 불러올 수 없습니다.</p>
            <button type="button" onClick={() => void workspaceListQuery.refetch()}>
              다시 시도
            </button>
          </section>
        )}

        {!workspaceListQuery.isLoading && !workspaceListQuery.isError && workspaces.length > 0 && (
          <section className={styles.workspaceList} aria-label="내 워크스페이스">
            {ownedWorkspaces.length > 0 && (
              <div className={styles.workspaceSection}>
                <div className={styles.sectionHeading}>
                  <h2>내가 만든 워크스페이스</h2>
                  <span>{ownedWorkspaces.length}</span>
                </div>
                <div className={styles.workspaceGrid}>
                  {ownedWorkspaces.map((workspace) => (
                    <button
                      key={workspace.id}
                      className={styles.workspaceCard}
                      type="button"
                      onClick={() => void navigate(`/workspaces/${workspace.id}`)}
                    >
                      {workspace.imageUrl ? (
                        <img src={workspace.imageUrl} alt="" />
                      ) : (
                        <span className={styles.workspaceImageFallback} aria-hidden="true">
                          {workspace.name.slice(0, 1)}
                        </span>
                      )}
                      <span className={styles.workspaceCardContent}>
                        <strong>{workspace.name}</strong>
                        <small>{workspace.status === 'ARCHIVED' ? '보관됨' : '활성'}</small>
                      </span>
                      <span className={styles.roleBadge}>{getWorkspaceRoleLabel(workspace.role)}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {joinedWorkspaces.length > 0 && (
              <div className={styles.workspaceSection}>
                <div className={styles.sectionHeading}>
                  <h2>참여 중인 워크스페이스</h2>
                  <span>{joinedWorkspaces.length}</span>
                </div>
                <div className={styles.workspaceGrid}>
                  {joinedWorkspaces.map((workspace) => (
                    <button
                      key={workspace.id}
                      className={styles.workspaceCard}
                      type="button"
                      onClick={() => void navigate(`/workspaces/${workspace.id}`)}
                    >
                      {workspace.imageUrl ? (
                        <img src={workspace.imageUrl} alt="" />
                      ) : (
                        <span className={styles.workspaceImageFallback} aria-hidden="true">
                          {workspace.name.slice(0, 1)}
                        </span>
                      )}
                      <span className={styles.workspaceCardContent}>
                        <strong>{workspace.name}</strong>
                        <small>{workspace.status === 'ARCHIVED' ? '보관됨' : '활성'}</small>
                      </span>
                      <span className={styles.roleBadge}>{getWorkspaceRoleLabel(workspace.role)}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </section>
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
