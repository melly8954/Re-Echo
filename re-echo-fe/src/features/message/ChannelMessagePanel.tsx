import type { FormEvent, KeyboardEvent } from 'react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../shared/api/apiTypes'
import { useChannelRealtime } from './useChannelRealtime'
import {
  channelMessagesQueryKey,
  useChannelMessages,
  useCreateChannelMessage,
} from './useChannelMessages'
import styles from './ChannelMessagePanel.module.css'

interface ChannelMessagePanelProps {
  workspaceId: string
  channelId: string
  currentMembershipId: string
  channelName: string
  readOnly: boolean
}

// REST 메시지 목록과 composer를 채널 화면에 연결한다.
export function ChannelMessagePanel({
  workspaceId,
  channelId,
  currentMembershipId,
  channelName,
  readOnly,
}: ChannelMessagePanelProps) {
  const messageListRef = useRef<HTMLDivElement>(null)
  const previousScrollHeightRef = useRef(0)
  const typingTimeoutRef = useRef<number | null>(null)
  const [content, setContent] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [typingUserIds, setTypingUserIds] = useState<string[]>([])
  const queryClient = useQueryClient()
  const messagesQuery = useChannelMessages(workspaceId, channelId, !readOnly)
  const createMessage = useCreateChannelMessage()
  const { publishTyping } = useChannelRealtime({
    workspaceId,
    channelId,
    enabled: !readOnly,
    onEvent: (event) => {
      if (event.type === 'TYPING_UPDATED') {
        setTypingUserIds(event.payload.typingUserIds)
        return
      }
      void queryClient.invalidateQueries({
        queryKey: channelMessagesQueryKey(workspaceId, channelId),
      })
    },
  })
  const messages = useMemo(
    () => messagesQuery.data?.pages.flatMap((page) => page.contents).reverse() ?? [],
    [messagesQuery.data],
  )

  useEffect(() => {
    const messageList = messageListRef.current
    if (!messageList || messages.length === 0) {
      return
    }
    if (previousScrollHeightRef.current === 0) {
      messageList.scrollTop = messageList.scrollHeight
    }
    previousScrollHeightRef.current = messageList.scrollHeight
  }, [messages.length])

  useEffect(() => () => {
    if (typingTimeoutRef.current !== null) {
      window.clearTimeout(typingTimeoutRef.current)
    }
  }, [])

  async function loadOlderMessages() {
    const messageList = messageListRef.current
    if (!messageList || !messagesQuery.hasNextPage || messagesQuery.isFetchingNextPage) {
      return
    }
    const previousScrollHeight = messageList.scrollHeight
    await messagesQuery.fetchNextPage()
    requestAnimationFrame(() => {
      messageList.scrollTop = messageList.scrollHeight - previousScrollHeight
    })
  }

  function handleMessageListScroll() {
    if (messageListRef.current?.scrollTop === 0) {
      void loadOlderMessages()
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedContent = content.trim()
    if (!trimmedContent || readOnly || createMessage.isPending) {
      return
    }
    setSubmitError(null)
    try {
      await createMessage.mutateAsync({
        workspaceId,
        channelId,
        request: { content: trimmedContent },
      })
      setContent('')
    } catch (error) {
      setSubmitError(
        error instanceof ApiError ? error.message : '메시지를 전송하지 못했습니다.',
      )
    }
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey) {
      return
    }
    event.preventDefault()
    event.currentTarget.form?.requestSubmit()
  }

  function handleContentChange(nextContent: string) {
    setContent(nextContent)
    publishTyping(Boolean(nextContent.trim()))
    if (typingTimeoutRef.current !== null) {
      window.clearTimeout(typingTimeoutRef.current)
    }
    if (nextContent.trim()) {
      typingTimeoutRef.current = window.setTimeout(() => publishTyping(false), 1000)
    }
  }

  if (messagesQuery.isLoading) {
    return <p className={styles.loading}>메시지를 불러오는 중입니다.</p>
  }

  if (messagesQuery.isError) {
    return (
      <div className={styles.error} role="alert">
        <p>
          {messagesQuery.error instanceof ApiError
            ? messagesQuery.error.message
            : '메시지를 불러오지 못했습니다.'}
        </p>
        <button type="button" onClick={() => void messagesQuery.refetch()}>
          다시 시도
        </button>
      </div>
    )
  }

  return (
    <div className={styles.panel}>
      <div
        ref={messageListRef}
        className={styles.messageList}
        aria-label={`${channelName} 메시지 목록`}
        onScroll={handleMessageListScroll}
      >
        {messagesQuery.isFetchingNextPage && <p className={styles.historyLoading}>이전 메시지를 불러오는 중입니다.</p>}
        {!messagesQuery.hasNextPage && messages.length > 0 && <p className={styles.historyEnd}>대화의 시작입니다.</p>}
        {messages.length === 0 && <p className={styles.empty}>첫 메시지를 보내 대화를 시작해 보세요.</p>}
        {messages.map((message) => {
          const isMine = message.author.memberId === currentMembershipId
          return (
            <article key={message.id} className={`${styles.message} ${isMine ? styles.myMessage : ''}`}>
              {message.author.profileImageUrl ? (
                <img src={message.author.profileImageUrl} alt="" className={styles.avatar} />
              ) : (
                <span className={styles.avatarFallback} aria-hidden="true">
                  {message.author.displayName.slice(0, 1)}
                </span>
              )}
              <div className={styles.messageContent}>
                <div className={styles.messageMeta}>
                  <strong>{message.author.displayName}</strong>
                  <time dateTime={message.createdAt}>{formatMessageTime(message.createdAt)}</time>
                  {message.edited && !message.deleted && <span>수정됨</span>}
                </div>
                <p className={message.deleted ? styles.deletedContent : undefined}>
                  {message.deleted ? '삭제된 메시지입니다.' : message.content}
                </p>
              </div>
            </article>
          )
        })}
      </div>
      <form className={styles.composer} onSubmit={handleSubmit}>
        <label className={styles.composerLabel} htmlFor="channel-message-content">
          {readOnly ? '보관된 채널에서는 메시지를 작성할 수 없습니다.' : `${channelName}에 메시지 보내기`}
        </label>
        <div className={styles.composerField}>
          <textarea
            id="channel-message-content"
            value={content}
            onChange={(event) => handleContentChange(event.target.value)}
            onKeyDown={handleComposerKeyDown}
            placeholder={readOnly ? '보관된 채널입니다.' : '메시지를 입력하세요.'}
            disabled={readOnly || createMessage.isPending}
            rows={1}
          />
          <button type="submit" disabled={readOnly || createMessage.isPending || !content.trim()}>
            {createMessage.isPending ? '전송 중' : '전송'}
          </button>
        </div>
        {submitError && <p className={styles.submitError} role="alert">{submitError}</p>}
      </form>
      {typingUserIds.filter((userId) => userId !== currentMembershipId).length > 0 && (
        <p className={styles.typing} role="status">
          다른 참여자가 입력 중입니다.
        </p>
      )}
    </div>
  )
}

function formatMessageTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
