import { apiRequest } from '../../shared/api/apiClient'

export interface ChannelMessageAuthor {
  memberId: string
  displayName: string
  profileImageUrl: string | null
}

export interface ChannelMessageAttachment {
  fileId: string
  fileName: string
  contentType: string
  size: number
  previewImage: boolean
}

export interface ChannelMessage {
  id: string
  channelId: string
  author: ChannelMessageAuthor
  content: string
  attachments: ChannelMessageAttachment[]
  createdAt: string
  updatedAt: string
  edited: boolean
  deleted: boolean
}

export interface MessageCursor {
  createdAt: string
  messageId: string
}

export interface ChannelMessagePage {
  contents: ChannelMessage[]
  page: {
    type: 'CURSOR'
    size: number
    hasNext: boolean
    nextCursor: MessageCursor | null
  }
}

export interface CreateChannelMessageRequest {
  content: string
  fileIds?: string[]
}

export function getChannelMessages(
  workspaceId: string,
  channelId: string,
  cursor: MessageCursor | null,
) {
  const parameters = new URLSearchParams({ size: '30' })
  if (cursor) {
    parameters.set('cursorCreatedAt', cursor.createdAt)
    parameters.set('cursorMessageId', cursor.messageId)
  }
  return apiRequest<ChannelMessagePage>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/messages?${parameters.toString()}`,
    {
      method: 'GET',
      authenticated: true,
    },
  )
}

export function createChannelMessage(
  workspaceId: string,
  channelId: string,
  request: CreateChannelMessageRequest,
) {
  return apiRequest<ChannelMessage>(
    `/api/v1/workspaces/${workspaceId}/channels/${channelId}/messages`,
    {
      method: 'POST',
      authenticated: true,
      body: JSON.stringify(request),
    },
  )
}
