import { useMutation } from '@tanstack/react-query'
import { joinWorkspaceByInviteLink } from './workspaceApi'

// 공개 초대 확인 뒤 워크스페이스 참여를 요청하는 mutation을 제공한다.
export function useJoinWorkspaceInvite() {
  return useMutation({
    mutationFn: joinWorkspaceByInviteLink,
  })
}
