import { useMutation } from '@tanstack/react-query'
import { leaveWorkspace } from './workspaceApi'

interface LeaveWorkspaceVariables {
  workspaceId: string
  memberId: string
}

// 현재 멤버의 워크스페이스 탈퇴 뒤 관련 목록 캐시를 정리한다.
export function useLeaveWorkspace() {
  return useMutation({
    mutationFn: ({ workspaceId, memberId }: LeaveWorkspaceVariables) =>
      leaveWorkspace(workspaceId, memberId),
  })
}
