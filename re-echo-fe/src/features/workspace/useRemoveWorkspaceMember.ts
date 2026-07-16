import { useMutation, useQueryClient } from '@tanstack/react-query'
import { removeWorkspaceMember } from './workspaceApi'
import { workspaceMembersQueryKey } from './useWorkspaceMembers'

// 강제 제거 뒤 목록 캐시를 갱신해 제거된 멤버를 즉시 화면에서 제외한다.
export function useRemoveWorkspaceMember() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ workspaceId, memberId }: { workspaceId: string; memberId: string }) =>
      removeWorkspaceMember(workspaceId, memberId),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({
        queryKey: workspaceMembersQueryKey(variables.workspaceId),
      })
    },
  })
}
