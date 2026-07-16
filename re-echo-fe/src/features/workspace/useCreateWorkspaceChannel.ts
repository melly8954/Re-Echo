import { useMutation } from '@tanstack/react-query'
import {
  createWorkspaceChannel,
  type CreateWorkspaceChannelRequest,
} from './workspaceApi'

interface CreateWorkspaceChannelVariables {
  workspaceId: string
  request: CreateWorkspaceChannelRequest
}

// 채널 생성 성공 후 현재 워크스페이스의 채널 목록을 최신화한다.
export function useCreateWorkspaceChannel() {
  return useMutation({
    mutationFn: ({ workspaceId, request }: CreateWorkspaceChannelVariables) =>
      createWorkspaceChannel(workspaceId, request),
  })
}
