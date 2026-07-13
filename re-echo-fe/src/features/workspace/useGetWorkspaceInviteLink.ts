import { useMutation } from '@tanstack/react-query'
import { getActiveWorkspaceInviteLink } from './workspaceApi'

export function useGetWorkspaceInviteLink() {
  return useMutation({
    mutationFn: getActiveWorkspaceInviteLink,
  })
}
