import { useQuery } from '@tanstack/react-query'
import { getWorkspaceInvitePreview } from './workspaceApi'

// 로그인 전 초대 대상 확인에 필요한 공개 미리보기 정보를 조회한다.
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
