import { useMutation } from '@tanstack/react-query'
import {
  addWorkspacePrivateChannelMembers,
  type AddWorkspaceChannelMembersRequest,
} from './workspaceApi'

interface AddWorkspacePrivateChannelMembersVariables {
  workspaceId: string
  channelId: string
  request: AddWorkspaceChannelMembersRequest
}

// 비공개 채널의 초기 또는 추가 멤버를 변경하고 채널 멤버 캐시를 갱신한다.
export function useAddWorkspacePrivateChannelMembers() {
  return useMutation({
    mutationFn: ({
      workspaceId,
      channelId,
      request,
    }: AddWorkspacePrivateChannelMembersVariables) =>
      addWorkspacePrivateChannelMembers(workspaceId, channelId, request),
  })
}
