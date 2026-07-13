import { useMutation } from '@tanstack/react-query'
import {
  addWorkspacePrivateChannelMembers,
  type AddWorkspaceChannelMembersRequest,
} from './workspaceApi'

interface AddWorkspacePrivateChannelMembersVariables {
  workspaceId: string
  channelId: string
  request: AddWorkspaceChannelMembersRequest
}

export function useAddWorkspacePrivateChannelMembers() {
  return useMutation({
    mutationFn: ({
      workspaceId,
      channelId,
      request,
    }: AddWorkspacePrivateChannelMembersVariables) =>
      addWorkspacePrivateChannelMembers(workspaceId, channelId, request),
  })
}
