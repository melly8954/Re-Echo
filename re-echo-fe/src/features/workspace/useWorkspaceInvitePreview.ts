import { useQuery } from '@tanstack/react-query'
import { getWorkspaceInvitePreview } from './workspaceApi'

export const workspaceInvitePreviewQueryKey = (token: string) => [
  'workspace-invite',
  token,
] as const

export function useWorkspaceInvitePreview(token: string) {
  return useQuery({
    queryKey: workspaceInvitePreviewQueryKey(token),
    queryFn: () => getWorkspaceInvitePreview(token),
    enabled: Boolean(token),
    retry: false,
  })
}
