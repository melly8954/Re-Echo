import { apiRequest } from '../../shared/api/apiClient'

export interface CreateWorkspaceRequest {
  name: string
  description?: string | null
}

export interface CreatedWorkspace {
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
