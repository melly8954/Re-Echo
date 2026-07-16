import { useEffect, useState } from 'react'
import { useChangeWorkspaceMemberRole } from './useChangeWorkspaceMemberRole'
import { useRemoveWorkspaceMember } from './useRemoveWorkspaceMember'
import { useWorkspaceMembers } from './useWorkspaceMembers'
import { getWorkspaceRoleLabel } from './workspaceLabels'
import type { WorkspaceMember, WorkspaceMembershipRole } from './workspaceApi'
import { ApiError } from '../../shared/api/apiTypes'
import styles from './WorkspaceMemberManagementDialog.module.css'

interface WorkspaceMemberManagementDialogProps {
  workspaceId: string
  workspaceName: string
  currentMembership: {
    id: string
    role: WorkspaceMembershipRole
  }
  isOpen: boolean
  onClose: () => void
}

// 워크스페이스 권한자가 역할과 강제 제거를 한 곳에서 관리하도록 제공한다.
export function WorkspaceMemberManagementDialog({
  workspaceId,
  workspaceName,
  currentMembership,
  isOpen,
  onClose,
}: WorkspaceMemberManagementDialogProps) {
  const workspaceMembersQuery = useWorkspaceMembers(workspaceId)
  const changeMemberRole = useChangeWorkspaceMemberRole()
  const removeMember = useRemoveWorkspaceMember()
  const resetChangeMemberRole = changeMemberRole.reset
  const resetRemoveMember = removeMember.reset
  const [removalCandidate, setRemovalCandidate] = useState<WorkspaceMember | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const members = workspaceMembersQuery.data?.contents ?? []
  const canChangeRole = currentMembership.role === 'OWNER'
  const canRemoveMember =
    currentMembership.role === 'OWNER' || currentMembership.role === 'ADMIN'
  const memberGroups = [
    {
      label: '소유자',
      members: members.filter((member) => member.role === 'OWNER'),
    },
    {
      label: '관리자',
      members: members.filter((member) => member.role === 'ADMIN'),
    },
    {
      label: '멤버',
      members: members.filter((member) => member.role === 'MEMBER'),
    },
  ]

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !changeMemberRole.isPending && !removeMember.isPending) {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [changeMemberRole.isPending, isOpen, onClose, removeMember.isPending])

  useEffect(() => {
    if (!isOpen) {
      setRemovalCandidate(null)
      setActionError(null)
      resetChangeMemberRole()
      resetRemoveMember()
    }
  }, [isOpen, resetChangeMemberRole, resetRemoveMember])

  if (!isOpen) {
    return null
  }

  async function handleRoleChange(memberId: string, role: WorkspaceMembershipRole) {
    setActionError(null)
    try {
      await changeMemberRole.mutateAsync({
        workspaceId,
        memberId,
        request: { role },
      })
    } catch (error) {
      setActionError(
        error instanceof ApiError ? error.message : '멤버 역할을 변경하지 못했습니다.',
      )
    }
  }

  async function handleRemoveMember() {
    if (!removalCandidate) {
      return
    }

    setActionError(null)
    try {
      await removeMember.mutateAsync({
        workspaceId,
        memberId: removalCandidate.id,
      })
      setRemovalCandidate(null)
    } catch (error) {
      setActionError(
        error instanceof ApiError ? error.message : '멤버를 제거하지 못했습니다.',
      )
    }
  }

  function closeDialog() {
    if (!changeMemberRole.isPending && !removeMember.isPending) {
      onClose()
    }
  }

  return (
    <div className={styles.layer}>
      <button
        className={styles.overlay}
        type="button"
        aria-label="멤버 관리 닫기"
        onClick={closeDialog}
      />
      <section
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="workspace-member-management-title"
      >
        <header className={styles.header}>
          <div>
            <p>워크스페이스 관리</p>
            <h2 id="workspace-member-management-title">멤버 관리</h2>
            <span>{workspaceName}</span>
          </div>
          <button type="button" onClick={closeDialog} disabled={changeMemberRole.isPending || removeMember.isPending}>
            닫기
          </button>
        </header>

        {actionError && <p className={styles.error} role="alert">{actionError}</p>}

        {workspaceMembersQuery.isLoading && (
          <div className={styles.skeletonList} aria-label="멤버 목록을 불러오는 중">
            <span />
            <span />
            <span />
          </div>
        )}

        {workspaceMembersQuery.isError && (
          <div className={styles.emptyState}>
            <p>멤버 목록을 불러올 수 없습니다.</p>
            <button type="button" onClick={() => void workspaceMembersQuery.refetch()}>
              다시 시도
            </button>
          </div>
        )}

        {!workspaceMembersQuery.isLoading && !workspaceMembersQuery.isError && (
          <div className={styles.groups}>
            {memberGroups.map((group) => (
              <section key={group.label} className={styles.group} aria-label={group.label}>
                <h3>{group.label}<span>{group.members.length}</span></h3>
                {group.members.map((member) => {
                  const isCurrentMember = member.id === currentMembership.id
                  const canEditMemberRole = canChangeRole && member.role !== 'OWNER'
                  const canRemoveTarget =
                    canRemoveMember && !isCurrentMember && member.role !== 'OWNER'

                  return (
                    <article key={member.id} className={styles.memberItem}>
                      {member.profileImageUrl ? (
                        <img src={member.profileImageUrl} alt="" />
                      ) : (
                        <span className={styles.avatarFallback} aria-hidden="true">
                          {member.displayName.slice(0, 1)}
                        </span>
                      )}
                      <strong>{member.displayName}{isCurrentMember ? ' (나)' : ''}</strong>
                      {canEditMemberRole ? (
                        <label className={styles.roleSelectLabel}>
                          <select
                            aria-label={`${member.displayName} 역할`}
                            value={member.role}
                            disabled={changeMemberRole.isPending}
                            onChange={(event) => void handleRoleChange(
                              member.id,
                              event.target.value as WorkspaceMembershipRole,
                            )}
                          >
                            <option value="ADMIN">관리자</option>
                            <option value="MEMBER">멤버</option>
                          </select>
                        </label>
                      ) : (
                        <span className={styles.roleLabel}>{getWorkspaceRoleLabel(member.role)}</span>
                      )}
                      {canRemoveTarget && (
                        <button
                          type="button"
                          className={styles.removeButton}
                          onClick={() => {
                            setActionError(null)
                            setRemovalCandidate(member)
                          }}
                          disabled={removeMember.isPending}
                        >
                          제거
                        </button>
                      )}
                    </article>
                  )
                })}
              </section>
            ))}
          </div>
        )}

        {removalCandidate && (
          <section className={styles.confirmPanel} aria-live="polite">
            <strong>{removalCandidate.displayName}님을 제거하시겠습니까?</strong>
            <p>제거된 멤버는 모든 채널에서 나가며 초대 링크로 다시 참여할 수 없습니다.</p>
            <div>
              <button
                type="button"
                className={styles.cancelButton}
                onClick={() => setRemovalCandidate(null)}
                disabled={removeMember.isPending}
              >
                취소
              </button>
              <button
                type="button"
                className={styles.confirmButton}
                onClick={() => void handleRemoveMember()}
                disabled={removeMember.isPending}
              >
                {removeMember.isPending ? '제거 중' : '제거'}
              </button>
            </div>
          </section>
        )}
      </section>
    </div>
  )
}
