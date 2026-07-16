import { useQuery } from '@tanstack/react-query'
import { getWorkspaceChannelDetail } from './workspaceApi'

// 채널 화면과 설정 화면이 같은 상세 데이터를 재사용하도록 조회 상태를 관리한다.
export const workspaceChannelDetailQueryKey = (workspaceId: string, channelId: string) => [
  'workspace',
  workspaceId,
  'channel',
  channelId,
] as const

export function useWorkspaceChannelDetail(workspaceId: string, channelId: string) {
  return useQuery({
    queryKey: workspaceChannelDetailQueryKey(workspaceId, channelId),
    queryFn: () => getWorkspaceChannelDetail(workspaceId, channelId),
    enabled: Boolean(workspaceId && channelId),
  })
}
