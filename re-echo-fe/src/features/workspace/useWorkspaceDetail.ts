import { useQuery } from '@tanstack/react-query'
import { getWorkspaceDetail } from './workspaceApi'

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
