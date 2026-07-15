import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  createChannelMessage,
  getChannelMessages,
  type CreateChannelMessageRequest,
  type MessageCursor,
} from './messageApi'

export const channelMessagesQueryKey = (workspaceId: string, channelId: string) => [
  'workspace',
  workspaceId,
  'channel',
  channelId,
  'messages',
] as const

export function useChannelMessages(workspaceId: string, channelId: string, enabled: boolean) {
  return useInfiniteQuery({
    queryKey: channelMessagesQueryKey(workspaceId, channelId),
    queryFn: ({ pageParam }) => getChannelMessages(workspaceId, channelId, pageParam),
    initialPageParam: null as MessageCursor | null,
    getNextPageParam: (lastPage) =>
      lastPage.page.hasNext ? lastPage.page.nextCursor : undefined,
    enabled: Boolean(workspaceId && channelId && enabled),
  })
}

interface CreateChannelMessageVariables {
  workspaceId: string
  channelId: string
  request: CreateChannelMessageRequest
}

export function useCreateChannelMessage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ workspaceId, channelId, request }: CreateChannelMessageVariables) =>
      createChannelMessage(workspaceId, channelId, request),
    onSuccess: async (_message, variables) => {
      await queryClient.invalidateQueries({
        queryKey: channelMessagesQueryKey(variables.workspaceId, variables.channelId),
      })
    },
  })
}
