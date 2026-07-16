import { useMutation } from '@tanstack/react-query'
import { archiveWorkspaceChannel } from './workspaceApi'

interface ArchiveWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
}

// 채널 보관 요청의 대기와 오류 상태를 화면에 제공한다.
export function useArchiveWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId }: ArchiveWorkspaceChannelVariables) =>
      archiveWorkspaceChannel(workspaceId, channelId),
  })
}
