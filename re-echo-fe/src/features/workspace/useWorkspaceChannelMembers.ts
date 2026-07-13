import { useQuery } from '@tanstack/react-query'
import { getWorkspaceChannelMembers } from './workspaceApi'

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
