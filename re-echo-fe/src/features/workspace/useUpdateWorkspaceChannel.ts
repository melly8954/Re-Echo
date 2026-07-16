import { useMutation } from '@tanstack/react-query'
import {
  updateWorkspaceChannel,
  type UpdateWorkspaceChannelRequest,
} from './workspaceApi'

interface UpdateWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
  request: UpdateWorkspaceChannelRequest
}

// 채널 설정 저장 요청의 대기와 오류 상태를 화면에 제공한다.
export function useUpdateWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId, request }: UpdateWorkspaceChannelVariables) =>
      updateWorkspaceChannel(workspaceId, channelId, request),
  })
}
