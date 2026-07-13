import { useMutation } from '@tanstack/react-query'
import { leaveWorkspace } from './workspaceApi'

interface LeaveWorkspaceVariables {
  workspaceId: string
  memberId: string
}

export function useLeaveWorkspace() {
  return useMutation({
    mutationFn: ({ workspaceId, memberId }: LeaveWorkspaceVariables) =>
      leaveWorkspace(workspaceId, memberId),
  })
}
