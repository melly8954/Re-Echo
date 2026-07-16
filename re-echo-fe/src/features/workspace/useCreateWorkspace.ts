import { useMutation } from '@tanstack/react-query'
import { createWorkspace } from './workspaceApi'

// 새 워크스페이스 생성 요청과 목록 캐시 갱신을 묶어 제공한다.
export function useCreateWorkspace() {
  return useMutation({
    mutationFn: createWorkspace,
  })
}
