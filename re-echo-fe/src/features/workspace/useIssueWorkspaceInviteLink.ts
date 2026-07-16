import { useMutation } from '@tanstack/react-query'
import { issueWorkspaceInviteLink } from './workspaceApi'

// 명시적인 초대 링크 재발급 요청과 기존 링크 캐시 교체를 관리한다.
export function useIssueWorkspaceInviteLink() {
  return useMutation({
    mutationFn: issueWorkspaceInviteLink,
  })
}
