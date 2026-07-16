import { useMutation } from '@tanstack/react-query'
import { getActiveWorkspaceInviteLink } from './workspaceApi'

// 기존 초대 링크를 재사용할 때 필요한 단건 조회 요청을 제공한다.
export function useGetWorkspaceInviteLink() {
  return useMutation({
    mutationFn: getActiveWorkspaceInviteLink,
  })
}
