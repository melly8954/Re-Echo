import type { FormEvent, KeyboardEvent, PointerEvent } from 'react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../shared/api/apiTypes'
import { useChannelRealtime } from './useChannelRealtime'
import {
  channelMessagesQueryKey,
  useChannelMessages,
  useCreateChannelMessage,
  useDeleteChannelMessage,
  useUpdateChannelMessage,
} from './useChannelMessages'
import type { ChannelMessage } from './messageApi'
import styles from './ChannelMessagePanel.module.css'

interface ChannelMessagePanelProps {
  workspaceId: string
  channelId: string
  currentMembershipId: string
  canManageMessages: boolean
  channelName: string
  readOnly: boolean
}

// REST 메시지 목록과 composer를 채널 화면에 연결한다.
export function ChannelMessagePanel({
  workspaceId,
  channelId,
  currentMembershipId,
  canManageMessages,
  channelName,
  readOnly,
}: ChannelMessagePanelProps) {
  const messageListRef = useRef<HTMLDivElement>(null)
  const composerInputRef = useRef<HTMLTextAreaElement>(null)
  const typingTimeoutRef = useRef<number | null>(null)
  const messageLongPressTimeoutRef = useRef<number | null>(null)
  const [content, setContent] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [typingUserIds, setTypingUserIds] = useState<string[]>([])
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null)
  const [editingContent, setEditingContent] = useState('')
  const [openMenuId, setOpenMenuId] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ChannelMessage | null>(null)
  const [messageActionError, setMessageActionError] = useState<{
    messageId: string
    message: string
  } | null>(null)
  const queryClient = useQueryClient()
  const messagesQuery = useChannelMessages(workspaceId, channelId, !readOnly)
  const createMessage = useCreateChannelMessage()
  const updateMessage = useUpdateChannelMessage()
  const deleteMessage = useDeleteChannelMessage()
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
  const latestMessageId = messages[messages.length - 1]?.id

  useLayoutEffect(() => {
    const messageList = messageListRef.current
    if (!messageList || messages.length === 0) {
      return
    }
    const animationFrame = window.requestAnimationFrame(() => {
      messageList.scrollTop = messageList.scrollHeight
    })
    return () => window.cancelAnimationFrame(animationFrame)
  }, [channelId, latestMessageId, messages.length, workspaceId])

  useEffect(() => () => {
    if (typingTimeoutRef.current !== null) {
      window.clearTimeout(typingTimeoutRef.current)
    }
    if (messageLongPressTimeoutRef.current !== null) {
      window.clearTimeout(messageLongPressTimeoutRef.current)
    }
  }, [])

  useEffect(() => {
    setEditingMessageId(null)
    setEditingContent('')
    setOpenMenuId(null)
    setDeleteTarget(null)
    setMessageActionError(null)
  }, [channelId, workspaceId])

  useEffect(() => {
    if (!deleteTarget) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape' && !deleteMessage.isPending) {
        setDeleteTarget(null)
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [deleteMessage.isPending, deleteTarget])

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
      window.requestAnimationFrame(() => composerInputRef.current?.focus())
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

  function startMessageEdit(message: ChannelMessage) {
    setMessageActionError(null)
    setOpenMenuId(null)
    setEditingMessageId(message.id)
    setEditingContent(message.content)
  }

  function cancelMessageEdit() {
    if (updateMessage.isPending) {
      return
    }
    setEditingMessageId(null)
    setEditingContent('')
  }

  async function handleMessageUpdate(event: FormEvent<HTMLFormElement>, message: ChannelMessage) {
    event.preventDefault()
    const trimmedContent = editingContent.trim()
    if (!trimmedContent || updateMessage.isPending) {
      return
    }

    setMessageActionError(null)
    try {
      await updateMessage.mutateAsync({
        workspaceId,
        channelId,
        messageId: message.id,
        request: {
          content: trimmedContent,
          fileIds: message.attachments.map((attachment) => attachment.fileId),
        },
      })
      setEditingMessageId(null)
      setEditingContent('')
    } catch (error) {
      setMessageActionError({
        messageId: message.id,
        message: error instanceof ApiError ? error.message : '메시지를 수정하지 못했습니다.',
      })
    }
  }

  function openDeleteDialog(message: ChannelMessage) {
    setMessageActionError(null)
    setOpenMenuId(null)
    deleteMessage.reset()
    setDeleteTarget(message)
  }

  function startMessageLongPress(event: PointerEvent<HTMLElement>, messageId: string) {
    if (event.pointerType !== 'touch') {
      return
    }
    clearMessageLongPress()
    messageLongPressTimeoutRef.current = window.setTimeout(() => {
      setOpenMenuId(messageId)
      messageLongPressTimeoutRef.current = null
    }, 500)
  }

  function clearMessageLongPress() {
    if (messageLongPressTimeoutRef.current !== null) {
      window.clearTimeout(messageLongPressTimeoutRef.current)
      messageLongPressTimeoutRef.current = null
    }
  }

  function closeDeleteDialog() {
    if (deleteMessage.isPending) {
      return
    }
    deleteMessage.reset()
    setDeleteTarget(null)
  }

  async function handleMessageDelete() {
    if (!deleteTarget) {
      return
    }

    try {
      await deleteMessage.mutateAsync({
        workspaceId,
        channelId,
        messageId: deleteTarget.id,
      })
      setDeleteTarget(null)
    } catch (error) {
      setMessageActionError({
        messageId: deleteTarget.id,
        message: error instanceof ApiError ? error.message : '메시지를 삭제하지 못했습니다.',
      })
      setDeleteTarget(null)
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
          const isEditing = editingMessageId === message.id
          const canDeleteMessage = isMine || canManageMessages
          return (
            <article
              key={message.id}
              className={`${styles.message} ${isMine ? styles.myMessage : ''}`}
              tabIndex={!message.deleted && !isEditing && !readOnly && canDeleteMessage ? 0 : undefined}
              onContextMenu={(event) => {
                if (!message.deleted && !isEditing && !readOnly && canDeleteMessage) {
                  event.preventDefault()
                  setOpenMenuId(message.id)
                }
              }}
              onPointerDown={(event) => {
                if (!message.deleted && !isEditing && !readOnly && canDeleteMessage) {
                  startMessageLongPress(event, message.id)
                }
              }}
              onPointerUp={clearMessageLongPress}
              onPointerCancel={clearMessageLongPress}
              onPointerMove={clearMessageLongPress}
              onKeyDown={(event) => {
                if (
                  !message.deleted &&
                  !isEditing &&
                  !readOnly &&
                  canDeleteMessage &&
                  (event.key === 'ContextMenu' || (event.shiftKey && event.key === 'F10'))
                ) {
                  event.preventDefault()
                  setOpenMenuId(message.id)
                }
              }}
            >
              {!isMine && message.author.profileImageUrl ? (
                <img src={message.author.profileImageUrl} alt="" className={styles.avatar} />
              ) : !isMine ? (
                <span className={styles.avatarFallback} aria-hidden="true">
                  {message.author.displayName.slice(0, 1)}
                </span>
              ) : null}
              <div className={styles.messageBody}>
                <div className={styles.messageContent}>
                  <div className={styles.messageMeta}>
                    <strong>{isMine ? '(나)' : message.author.displayName}</strong>
                    {message.edited && !message.deleted && <span>수정됨</span>}
                  </div>
                {isEditing ? (
                  <form className={styles.messageEditForm} onSubmit={(event) => void handleMessageUpdate(event, message)}>
                    <textarea
                      aria-label="메시지 수정"
                      value={editingContent}
                      onChange={(event) => setEditingContent(event.target.value)}
                      disabled={updateMessage.isPending}
                      rows={2}
                    />
                    <div className={styles.messageEditActions}>
                      <button type="button" onClick={cancelMessageEdit} disabled={updateMessage.isPending}>
                        취소
                      </button>
                      <button type="submit" disabled={updateMessage.isPending || !editingContent.trim()}>
                        {updateMessage.isPending ? '저장 중' : '저장'}
                      </button>
                    </div>
                  </form>
                ) : (
                  <p className={message.deleted ? styles.deletedContent : undefined}>
                    {message.deleted ? '삭제된 메시지입니다.' : message.content}
                  </p>
                )}
                  {!message.deleted && !isEditing && !readOnly && canDeleteMessage && openMenuId === message.id && (
                    <div className={styles.messageActionPopup} role="menu" aria-label="메시지 작업 메뉴">
                      {isMine && (
                        <button type="button" role="menuitem" onClick={() => startMessageEdit(message)}>
                          수정
                        </button>
                      )}
                      <button type="button" role="menuitem" className={styles.deleteButton} onClick={() => openDeleteDialog(message)}>
                        삭제
                      </button>
                    </div>
                  )}
                  {messageActionError?.messageId === message.id && (
                    <p className={styles.messageActionError} role="alert">
                      {messageActionError.message}
                    </p>
                  )}
                </div>
                <time className={styles.messageTime} dateTime={message.createdAt}>
                  {formatMessageTime(message.createdAt)}
                </time>
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
            ref={composerInputRef}
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
      {deleteTarget && (
        <div className={styles.deleteDialogLayer}>
          <button
            className={styles.deleteDialogOverlay}
            type="button"
            aria-label="메시지 삭제 닫기"
            onClick={closeDeleteDialog}
          />
          <section
            className={styles.deleteDialog}
            role="dialog"
            aria-modal="true"
            aria-labelledby="message-delete-title"
            aria-describedby="message-delete-description"
          >
            <p className={styles.dialogEyebrow}>메시지 삭제</p>
            <h2 id="message-delete-title">이 메시지를 삭제하시겠습니까?</h2>
            <p id="message-delete-description">
              삭제된 메시지는 대화에 삭제 흔적으로 남습니다.
            </p>
            {deleteMessage.isError && (
              <p className={styles.messageActionError} role="alert">
                {deleteMessage.error instanceof ApiError
                  ? deleteMessage.error.message
                  : '메시지를 삭제하지 못했습니다.'}
              </p>
            )}
            <div className={styles.deleteDialogActions}>
              <button type="button" onClick={closeDeleteDialog} disabled={deleteMessage.isPending}>
                취소
              </button>
              <button type="button" className={styles.deleteButton} onClick={() => void handleMessageDelete()} disabled={deleteMessage.isPending}>
                {deleteMessage.isPending ? '삭제 중' : '삭제'}
              </button>
            </div>
          </section>
        </div>
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
