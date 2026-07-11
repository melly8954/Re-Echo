import { apiRequest } from '../../shared/api/apiClient'

export interface CreateWorkspaceRequest {
  name: string
  description?: string | null
}

export interface CreatedWorkspace {
  id: string
  defaultChannelId: string
}

export type WorkspaceStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED'
export type WorkspaceMembershipRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type WorkspaceMembershipStatus = 'ACTIVE' | 'LEFT' | 'REMOVED'

export interface WorkspaceDetail {
  id: string
  name: string
  description: string | null
  imageUrl: string | null
  status: WorkspaceStatus
  myMembership: {
    id: string
    displayName: string
    profileImageUrl: string | null
    role: WorkspaceMembershipRole
    status: WorkspaceMembershipStatus
  }
  defaultChannelId: string
  canRestore: boolean
}

export function createWorkspace(request: CreateWorkspaceRequest) {
  return apiRequest<CreatedWorkspace>('/api/v1/workspaces', {
    method: 'POST',
    authenticated: true,
    body: JSON.stringify(request),
  })
}

export function getWorkspaceDetail(workspaceId: string) {
  return apiRequest<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    method: 'GET',
    authenticated: true,
  })
}
