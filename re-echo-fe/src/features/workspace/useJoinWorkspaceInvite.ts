import { useMutation } from '@tanstack/react-query'
import { joinWorkspaceByInviteLink } from './workspaceApi'

export function useJoinWorkspaceInvite() {
  return useMutation({
    mutationFn: joinWorkspaceByInviteLink,
  })
}
