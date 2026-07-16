import { useMutation } from '@tanstack/react-query'
import { restoreWorkspaceChannel } from './workspaceApi'

interface RestoreWorkspaceChannelVariables {
  workspaceId: string
  channelId: string
}

// 보관된 채널의 복원 요청 상태를 화면에 제공한다.
export function useRestoreWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, channelId }: RestoreWorkspaceChannelVariables) =>
      restoreWorkspaceChannel(workspaceId, channelId),
  })
}
