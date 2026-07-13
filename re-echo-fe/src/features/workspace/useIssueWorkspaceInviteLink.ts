import { useMutation } from '@tanstack/react-query'
import { issueWorkspaceInviteLink } from './workspaceApi'

export function useIssueWorkspaceInviteLink() {
  return useMutation({
    mutationFn: issueWorkspaceInviteLink,
  })
}
