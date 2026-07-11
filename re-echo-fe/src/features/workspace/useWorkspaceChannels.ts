import { useQuery } from '@tanstack/react-query'
import { getWorkspaceChannels } from './workspaceApi'

export const workspaceChannelsQueryKey = (workspaceId: string) => [
  'workspace',
  workspaceId,
  'channels',
] as const

export function useWorkspaceChannels(workspaceId: string) {
  return useQuery({
    queryKey: workspaceChannelsQueryKey(workspaceId),
    queryFn: () => getWorkspaceChannels(workspaceId),
    enabled: Boolean(workspaceId),
  })
}
