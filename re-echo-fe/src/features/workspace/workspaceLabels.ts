import type {
  WorkspaceMembershipRole,
  WorkspaceMembershipStatus,
  WorkspaceStatus,
} from './workspaceApi'

export function getWorkspaceRoleLabel(role: WorkspaceMembershipRole) {
  if (role === 'OWNER') {
    return '소유자'
  }
  if (role === 'ADMIN') {
    return '관리자'
  }
  return '멤버'
}

export function getWorkspaceStatusLabel(status: WorkspaceStatus) {
  if (status === 'ACTIVE') {
    return '운영 중'
  }
  if (status === 'ARCHIVED') {
    return '보관됨'
  }
  return '삭제됨'
}

export function getWorkspaceMembershipStatusLabel(status: WorkspaceMembershipStatus) {
  if (status === 'ACTIVE') {
    return '참여 중'
  }
  if (status === 'LEFT') {
    return '나감'
  }
  return '제거됨'
}
