import { useMutation } from '@tanstack/react-query'
import { archiveWorkspace } from './workspaceApi'

// 소유자의 워크스페이스 보관 요청 상태를 관리한다.
export function useArchiveWorkspace() {
  return useMutation({
    mutationFn: archiveWorkspace,
  })
}
