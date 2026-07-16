import { useQuery } from '@tanstack/react-query'
import { getWorkspaceMembers } from './workspaceApi'

// 워크스페이스 관리 화면의 멤버 목록을 별도 캐시 키로 관리한다.
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
