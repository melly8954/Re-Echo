import { useQuery } from '@tanstack/react-query'
import { getWorkspaces } from './workspaceApi'

export const workspaceListQueryKey = ['workspaces'] as const

export function useWorkspaceList() {
  return useQuery({
    queryKey: workspaceListQueryKey,
    queryFn: getWorkspaces,
  })
}
