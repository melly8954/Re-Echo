import { useQuery } from '@tanstack/react-query'
import { getWorkspaceDetail } from './workspaceApi'

// 현재 사용자의 멤버십을 포함한 워크스페이스 상세 정보를 조회한다.
export const workspaceDetailQueryKey = (workspaceId: string) => [
  'workspace',
  workspaceId,
] as const

export function useWorkspaceDetail(workspaceId: string) {
  return useQuery({
    queryKey: workspaceDetailQueryKey(workspaceId),
    queryFn: () => getWorkspaceDetail(workspaceId),
    enabled: Boolean(workspaceId),
  })
}
