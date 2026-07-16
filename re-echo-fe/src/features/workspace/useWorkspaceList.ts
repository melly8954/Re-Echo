import { useQuery } from '@tanstack/react-query'
import { getWorkspaces } from './workspaceApi'

// 로그인 사용자가 접근할 수 있는 워크스페이스 레일 목록을 조회한다.
export const workspaceListQueryKey = ['workspaces'] as const

export function useWorkspaceList(enabled = true) {
  return useQuery({
    queryKey: workspaceListQueryKey,
    queryFn: getWorkspaces,
    enabled,
  })
}
