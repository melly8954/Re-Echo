import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  changeWorkspaceMemberRole,
  type ChangeWorkspaceMemberRoleRequest,
} from './workspaceApi'
import { workspaceMembersQueryKey } from './useWorkspaceMembers'

// 멤버 역할 변경 뒤 목록 캐시를 갱신해 관리 화면의 권한을 일관되게 유지한다.
export function useChangeWorkspaceMemberRole() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      workspaceId,
      memberId,
      request,
    }: {
      workspaceId: string
      memberId: string
      request: ChangeWorkspaceMemberRoleRequest
    }) => changeWorkspaceMemberRole(workspaceId, memberId, request),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: workspaceMembersQueryKey(variables.workspaceId),
      })
    },
  })
}
