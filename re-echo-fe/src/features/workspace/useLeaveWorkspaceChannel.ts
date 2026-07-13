import { useMutation } from '@tanstack/react-query'
import { leaveWorkspaceChannel } from './workspaceApi'

interface LeaveWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
}

export function useLeaveWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId }: LeaveWorkspaceChannelVariables) =>
      leaveWorkspaceChannel(workspaceId, channelId),
  })
}
