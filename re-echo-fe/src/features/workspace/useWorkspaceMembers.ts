import { useQuery } from '@tanstack/react-query'
import { getWorkspaceMembers } from './workspaceApi'

export const workspaceMembersQueryKey = (workspaceId: string) => [
  'workspace',
  workspaceId,
  'members',
] as const

export function useWorkspaceMembers(workspaceId: string) {
  return useQuery({
    queryKey: workspaceMembersQueryKey(workspaceId),
    queryFn: () => getWorkspaceMembers(workspaceId),
    enabled: Boolean(workspaceId),
  })
}
