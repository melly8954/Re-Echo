import { useMutation } from '@tanstack/react-query'
import { leaveWorkspaceChannel } from './workspaceApi'

interface LeaveWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
}

// 채널 탈퇴 뒤 접근 가능한 채널과 채널 멤버 캐시를 동기화한다.
export function useLeaveWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId }: LeaveWorkspaceChannelVariables) =>
      leaveWorkspaceChannel(workspaceId, channelId),
  })
}
