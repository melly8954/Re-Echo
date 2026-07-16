import { useMutation } from '@tanstack/react-query'
import { joinWorkspaceChannel } from './workspaceApi'

interface JoinWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
}

// 채널 참여 후 채널 목록과 해당 채널의 멤버 캐시를 함께 갱신한다.
export function useJoinWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId }: JoinWorkspaceChannelVariables) =>
      joinWorkspaceChannel(workspaceId, channelId),
  })
}
