import { useQuery } from '@tanstack/react-query'
import { getWorkspaceChannelMembers } from './workspaceApi'

// 채널별 멤버 목록을 다른 워크스페이스 상태와 분리해 조회한다.
export const workspaceChannelMembersQueryKey = (
  workspaceId: string,
  channelId: string,
) => ['workspace', workspaceId, 'channels', channelId, 'members'] as const

export function useWorkspaceChannelMembers(
  workspaceId: string,
  channelId: string,
) {
  return useQuery({
    queryKey: workspaceChannelMembersQueryKey(workspaceId, channelId),
    queryFn: () => getWorkspaceChannelMembers(workspaceId, channelId),
    enabled: Boolean(workspaceId && channelId),
  })
}
