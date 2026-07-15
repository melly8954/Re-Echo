import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  createChannelMessage,
  deleteChannelMessage,
  getChannelMessages,
  updateChannelMessage,
  type CreateChannelMessageRequest,
  type MessageCursor,
  type UpdateChannelMessageRequest,
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

interface UpdateChannelMessageVariables {
  workspaceId: string
  channelId: string
  messageId: string
  request: UpdateChannelMessageRequest
}

export function useUpdateChannelMessage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ workspaceId, channelId, messageId, request }: UpdateChannelMessageVariables) =>
      updateChannelMessage(workspaceId, channelId, messageId, request),
    onSuccess: async (_message, variables) => {
      await queryClient.invalidateQueries({
        queryKey: channelMessagesQueryKey(variables.workspaceId, variables.channelId),
      })
    },
  })
}

interface DeleteChannelMessageVariables {
  workspaceId: string
  channelId: string
  messageId: string
}

export function useDeleteChannelMessage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ workspaceId, channelId, messageId }: DeleteChannelMessageVariables) =>
      deleteChannelMessage(workspaceId, channelId, messageId),
    onSuccess: async (_result, variables) => {
      await queryClient.invalidateQueries({
        queryKey: channelMessagesQueryKey(variables.workspaceId, variables.channelId),
      })
    },
  })
}
