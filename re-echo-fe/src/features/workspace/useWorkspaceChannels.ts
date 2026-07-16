import { useQuery } from '@tanstack/react-query'
import { getWorkspaceChannels } from './workspaceApi'

// 워크스페이스별 접근 가능 채널 목록을 조회하고 캐시한다.
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
