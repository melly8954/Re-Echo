import { useMutation } from '@tanstack/react-query'
import {
  createWorkspaceChannel,
  type CreateWorkspaceChannelRequest,
} from './workspaceApi'

interface CreateWorkspaceChannelVariables {
  workspaceId: string
  request: CreateWorkspaceChannelRequest
}

export function useCreateWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, request }: CreateWorkspaceChannelVariables) =>
      createWorkspaceChannel(workspaceId, request),
  })
}
