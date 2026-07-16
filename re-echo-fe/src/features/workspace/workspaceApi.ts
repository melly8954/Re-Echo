import { apiRequest } from '../../shared/api/apiClient'

export interface CreateWorkspaceRequest {
  name: string
  description?: string | null
}

export interface UpdateWorkspaceRequest {
  name: string
  description: string | null
  imageFileId: string | null
}

export interface UpdateWorkspaceProfileRequest {
  displayName: string
  profileImageFileId?: string | null
}

export interface WorkspaceImagePresignRequest {
  fileName: string
  contentType: string
  size: number
}

export interface PresignedUploadResponse {
  fileId: string
  uploadUrl: string
  expiresAt: string
}

export interface CreatedWorkspace {
  id: string
  defaultChannelId: string
}

export interface CreateWorkspaceChannelRequest {
  name: string
  description?: string | null
  visibility: ChannelVisibility
  memberIds?: string[]
}

export interface AddWorkspaceChannelMembersRequest {
  memberIds: string[]
}

export interface UpdateWorkspaceChannelRequest {
  name: string
  description: string | null
}

export interface CreatedWorkspaceChannel {
  id: string
}

export type WorkspaceStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED'
export type WorkspaceMembershipRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type WorkspaceMembershipStatus = 'ACTIVE' | 'LEFT' | 'REMOVED'
export type ChannelVisibility = 'PUBLIC' | 'PRIVATE'
export type ChannelStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED'

export interface WorkspaceDetail {
  id: string
  name: string
  description: string | null
  imageUrl: string | null
  imageFileId: string | null
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

export interface ChangeWorkspaceMemberRoleRequest {
  role: WorkspaceMembershipRole
}

export interface WorkspaceChannel {
  id: string
  name: string
  visibility: ChannelVisibility
  isGeneral: boolean
  joined: boolean
  createdByMe: boolean
  status: ChannelStatus
  archiveExpiresAt: string | null
  unreadCount: number
  memberCount: number
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

// 워크스페이스·채널·멤버·초대 링크의 REST 계약을 기능 계층에 제공한다.
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

export interface WorkspaceChannelDetail {
  id: string
  name: string
  description: string | null
  visibility: ChannelVisibility
  isGeneral: boolean
  joined: boolean
  createdByMe: boolean
  status: ChannelStatus
  archiveExpiresAt: string | null
}

export function updateWorkspace(
  workspaceId: string,
  request: UpdateWorkspaceRequest,
) {
  return apiRequest<WorkspaceDetail>(`/api/v1/workspaces/${workspaceId}`, {
    method: 'PATCH',
    authenticated: true,
    body: JSON.stringify(request),
  })
}

export function archiveWorkspace(workspaceId: string) {
  return apiRequest<null>(`/api/v1/workspaces/${workspaceId}/archive`, {
    method: 'PATCH',
    authenticated: true,
  })
}

export function restoreWorkspace(workspaceId: string) {
  return apiRequest<null>(`/api/v1/workspaces/${workspaceId}/restore`, {
    method: 'PATCH',
    authenticated: true,
  })
}

export function createWorkspaceImageUploadUrl(
  workspaceId: string,
  request: WorkspaceImagePresignRequest,
) {
  return apiRequest<PresignedUploadResponse>(
    `/api/v1/workspaces/${workspaceId}/image/presign-upload`,
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export async function uploadWorkspaceImageToStorage(uploadUrl: string, file: File) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  })

  if (!response.ok) {
    throw new Error('워크스페이스 대표 이미지 업로드에 실패했습니다.')
  }
}

export function updateWorkspaceProfile(
  workspaceId: string,
  request: UpdateWorkspaceProfileRequest,
) {
  return apiRequest<WorkspaceMember>(
    `/api/v1/workspaces/${workspaceId}/members/me/profile`,
    {
      method: 'PATCH',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export function createWorkspaceProfileImageUploadUrl(
  workspaceId: string,
  request: WorkspaceImagePresignRequest,
) {
  return apiRequest<PresignedUploadResponse>(
    `/api/v1/workspaces/${workspaceId}/members/me/profile-image/presign-upload`,
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export async function uploadWorkspaceProfileImageToStorage(uploadUrl: string, file: File) {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  })

  if (!response.ok) {
    throw new Error('워크스페이스 프로필 이미지 업로드에 실패했습니다.')
  }
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

export function createWorkspaceChannel(
  workspaceId: string,
  request: CreateWorkspaceChannelRequest,
) {
  return apiRequest<CreatedWorkspaceChannel>(
    `/api/v1/workspaces/${workspaceId}/channels`,
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export function getWorkspaceChannelDetail(workspaceId: string, channelId: string) {
  return apiRequest<WorkspaceChannelDetail>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}`,
    {
      method: 'GET',
      authenticated: true,
    },
  )
}

export function updateWorkspaceChannel(
  workspaceId: string,
  channelId: string,
  request: UpdateWorkspaceChannelRequest,
) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}`,
    {
      method: 'PATCH',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export function archiveWorkspaceChannel(workspaceId: string, channelId: string) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/archive`,
    {
      method: 'PATCH',
      authenticated: true,
    },
  )
}

export function restoreWorkspaceChannel(workspaceId: string, channelId: string) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/restore`,
    {
      method: 'PATCH',
      authenticated: true,
    },
  )
}

export function joinWorkspaceChannel(workspaceId: string, channelId: string) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/join`,
    {
      method: 'POST',
      authenticated: true,
    },
  )
}

export function leaveWorkspaceChannel(workspaceId: string, channelId: string) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/leave`,
    {
      method: 'POST',
      authenticated: true,
    },
  )
}

export function addWorkspacePrivateChannelMembers(
  workspaceId: string,
  channelId: string,
  request: AddWorkspaceChannelMembersRequest,
) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/members`,
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
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

export function changeWorkspaceMemberRole(
  workspaceId: string,
  memberId: string,
  request: ChangeWorkspaceMemberRoleRequest,
) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/members/${memberId}/role`,
    {
      method: 'PATCH',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}

export function removeWorkspaceMember(workspaceId: string, memberId: string) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/members/${memberId}/remove`,
    {
      method: 'POST',
      authenticated: true,
    },
  )
}

export function leaveWorkspace(workspaceId: string, memberId: string) {
  return apiRequest<null>(
    `/api/v1/workspaces/${workspaceId}/members/${memberId}/leave`,
    {
      method: 'POST',
      authenticated: true,
    },
  )
}

export function getWorkspaceChannelMembers(workspaceId: string, channelId: string) {
  return apiRequest<WorkspaceMemberList>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/members`,
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
