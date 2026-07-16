import { useMutation } from '@tanstack/react-query'
import { restoreWorkspace } from './workspaceApi'

// 소유자의 만료 전 워크스페이스 복원 요청 상태를 관리한다.
export function useRestoreWorkspace() {
  return useMutation({
    mutationFn: restoreWorkspace,
  })
}
