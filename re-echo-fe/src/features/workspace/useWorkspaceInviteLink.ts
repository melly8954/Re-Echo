import { useQuery } from '@tanstack/react-query'
import { getActiveWorkspaceInviteLink } from './workspaceApi'

export const workspaceInviteLinkQueryKey = (workspaceId: string) => [
  'workspace',
  workspaceId,
  'invite-link',
] as const

// 워크스페이스 홈에서 현재 활성 초대 링크를 조회한다.
export function useWorkspaceInviteLink(workspaceId: string, enabled: boolean) {
  return useQuery({
    queryKey: workspaceInviteLinkQueryKey(workspaceId),
    queryFn: () => getActiveWorkspaceInviteLink(workspaceId),
    enabled: Boolean(workspaceId) && enabled,
  })
}
