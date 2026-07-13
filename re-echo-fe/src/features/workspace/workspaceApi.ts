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
export type ChannelVisibility = 'PUBLIC' | 'PRIVATE'

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

export interface WorkspaceListItem {
  id: string
  name: string
  imageUrl: string | null
  role: WorkspaceMembershipRole
  status: WorkspaceStatus
  lastVisitedAt: string
  defaultChannelId: string
  unreadChannelCount: number
}

export interface WorkspaceList {
  contents: WorkspaceListItem[]
}

export interface WorkspaceMember {
  id: string
  displayName: string
  profileImageUrl: string | null
  role: WorkspaceMembershipRole
  status: WorkspaceMembershipStatus
}

export interface WorkspaceMemberList {
  contents: WorkspaceMember[]
}

export interface WorkspaceChannel {
  id: string
  name: string
  visibility: ChannelVisibility
  isGeneral: boolean
  joined: boolean
  unreadCount: number
}

export interface WorkspaceChannelList {
  contents: WorkspaceChannel[]
}

export interface WorkspaceInviteLink {
  token: string
  expiresAt: string
}

export interface WorkspaceInvitePreview {
  workspaceName: string
  workspaceImageUrl: string | null
  expiresAt: string
}

export interface JoinedWorkspace {
  id: string
  defaultChannelId: string
}

export function createWorkspace(request: CreateWorkspaceRequest) {
  return apiRequest<CreatedWorkspace>('/api/v1/workspaces', {
    method: 'POST',
    authenticated: true,
    body: JSON.stringify(request),
  })
}

export function getWorkspaces() {
  return apiRequest<WorkspaceList>('/api/v1/workspaces', {
    method: 'GET',
    authenticated: true,
  })
}

export function getWorkspaceDetail(workspaceId: string) {
  return apiRequest<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    method: 'GET',
    authenticated: true,
  })
}

export function getWorkspaceChannels(workspaceId: string) {
  return apiRequest<WorkspaceChannelList>(
    `/api/v1/workspaces/${workspaceId}/channels`,
    {
      method: 'GET',
      authenticated: true,
    },
  )
}

export function getWorkspaceMembers(workspaceId: string) {
  return apiRequest<WorkspaceMemberList>(
    `/api/v1/workspaces/${workspaceId}/members`,
    {
      method: 'GET',
      authenticated: true,
    },
  )
}

export function getActiveWorkspaceInviteLink(workspaceId: string) {
  return apiRequest<WorkspaceInviteLink>(
    `/api/v1/workspaces/${workspaceId}/invite-link`,
    {
      method: 'GET',
      authenticated: true,
    },
  )
}

export function issueWorkspaceInviteLink(workspaceId: string) {
  return apiRequest<WorkspaceInviteLink>(
    `/api/v1/workspaces/${workspaceId}/invite-link`,
    {
      method: 'POST',
      authenticated: true,
    },
  )
}

export function getWorkspaceInvitePreview(token: string) {
  return apiRequest<WorkspaceInvitePreview>(`/api/v1/invite-links/${token}`, {
    method: 'GET',
  })
}

export function joinWorkspaceByInviteLink(token: string) {
  return apiRequest<JoinedWorkspace>(`/api/v1/invite-links/${token}/join`, {
    method: 'POST',
    authenticated: true,
  })
}
