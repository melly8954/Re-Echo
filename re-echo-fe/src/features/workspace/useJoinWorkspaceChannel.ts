import { useMutation } from '@tanstack/react-query'
import { joinWorkspaceChannel } from './workspaceApi'

interface JoinWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
}

export function useJoinWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId }: JoinWorkspaceChannelVariables) =>
      joinWorkspaceChannel(workspaceId, channelId),
  })
}
