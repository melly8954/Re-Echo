import type { ChangeEvent, DragEvent, FormEvent, KeyboardEvent, PointerEvent } from 'react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../shared/api/apiTypes'
import { useChannelRealtime } from './useChannelRealtime'
import {
  channelMessagesQueryKey,
  useChannelMessages,
  useCreateChannelMessage,
  useDeleteChannelMessage,
  useUpdateChannelReadState,
  useUpdateChannelMessage,
} from './useChannelMessages'
import {
  createMessageAttachmentDownloadUrl,
  createMessageAttachmentUploadUrl,
  uploadMessageAttachmentToStorage,
} from './messageAttachmentApi'
import type { ChannelMessage, ChannelMessageAttachment } from './messageApi'
import styles from './ChannelMessagePanel.module.css'

const MESSAGE_GROUP_INTERVAL_MILLISECONDS = 5 * 60 * 1000
const MESSAGE_ATTACHMENT_MAX_SIZE_BYTES = 20 * 1024 * 1024

type AttachmentUploadStatus = 'UPLOADING' | 'UPLOADED' | 'FAILED'

interface PendingMessageAttachment {
  localId: string
  file: File
  previewUrl: string | null
  status: AttachmentUploadStatus
  progress: number
  fileId: string | null
  errorMessage: string | null
}

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
  const attachmentInputRef = useRef<HTMLInputElement>(null)
  const attachmentMenuRef = useRef<HTMLDivElement>(null)
  const messageActionPopupRef = useRef<HTMLDivElement>(null)
  const typingTimeoutRef = useRef<number | null>(null)
  const messageLongPressTimeoutRef = useRef<number | null>(null)
  const attachmentPreviewUrlsRef = useRef(new Set<string>())
  const lastReadMessageIdRef = useRef<string | null>(null)
  const [content, setContent] = useState('')
  const [attachments, setAttachments] = useState<PendingMessageAttachment[]>([])
  const [isAttachmentMenuOpen, setIsAttachmentMenuOpen] = useState(false)
  const [isComposerDragOver, setIsComposerDragOver] = useState(false)
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
  const { mutate: updateChannelReadState } = useUpdateChannelReadState()
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
  const isAttachmentUploading = attachments.some((attachment) => attachment.status === 'UPLOADING')
  const hasFailedAttachment = attachments.some((attachment) => attachment.status === 'FAILED')

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

  useEffect(() => {
    if (readOnly || !latestMessageId || lastReadMessageIdRef.current === latestMessageId) {
      return
    }

    lastReadMessageIdRef.current = latestMessageId
    updateChannelReadState(
      { workspaceId, channelId, lastReadMessageId: latestMessageId },
      {
        onError: () => {
          if (lastReadMessageIdRef.current === latestMessageId) {
            lastReadMessageIdRef.current = null
          }
        },
      },
    )
  }, [channelId, latestMessageId, readOnly, updateChannelReadState, workspaceId])

  useEffect(() => () => {
    if (typingTimeoutRef.current !== null) {
      window.clearTimeout(typingTimeoutRef.current)
    }
    if (messageLongPressTimeoutRef.current !== null) {
      window.clearTimeout(messageLongPressTimeoutRef.current)
    }
    attachmentPreviewUrlsRef.current.forEach((previewUrl) => URL.revokeObjectURL(previewUrl))
  }, [])

  useEffect(() => {
    lastReadMessageIdRef.current = null
    setEditingMessageId(null)
    setEditingContent('')
    setOpenMenuId(null)
    setDeleteTarget(null)
    setMessageActionError(null)
    setIsAttachmentMenuOpen(false)
    clearSelectedAttachments()
  }, [channelId, workspaceId])

  useEffect(() => {
    if (!isAttachmentMenuOpen) {
      return undefined
    }

    function closeAttachmentMenu(event: globalThis.PointerEvent) {
      if (!attachmentMenuRef.current?.contains(event.target as Node)) {
        setIsAttachmentMenuOpen(false)
      }
    }

    function closeAttachmentMenuOnEscape(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsAttachmentMenuOpen(false)
      }
    }

    document.addEventListener('pointerdown', closeAttachmentMenu)
    document.addEventListener('keydown', closeAttachmentMenuOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeAttachmentMenu)
      document.removeEventListener('keydown', closeAttachmentMenuOnEscape)
    }
  }, [isAttachmentMenuOpen])

  useEffect(() => {
    if (!openMenuId) {
      return undefined
    }

    function closeMessageActionMenu(event: globalThis.PointerEvent) {
      if (!messageActionPopupRef.current?.contains(event.target as Node)) {
        setOpenMenuId(null)
      }
    }

    function closeMessageActionMenuOnEscape(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpenMenuId(null)
      }
    }

    document.addEventListener('pointerdown', closeMessageActionMenu)
    document.addEventListener('keydown', closeMessageActionMenuOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeMessageActionMenu)
      document.removeEventListener('keydown', closeMessageActionMenuOnEscape)
    }
  }, [openMenuId])

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
    const uploadedFileIds = attachments
      .flatMap((attachment) => (
        attachment.status === 'UPLOADED' && attachment.fileId ? [attachment.fileId] : []
      ))
    if (
      (!trimmedContent && uploadedFileIds.length === 0)
      || readOnly
      || createMessage.isPending
      || isAttachmentUploading
      || hasFailedAttachment
    ) {
      return
    }
    setSubmitError(null)
    try {
      await createMessage.mutateAsync({
        workspaceId,
        channelId,
        request: { content: trimmedContent, fileIds: uploadedFileIds },
      })
      setContent('')
      clearSelectedAttachments()
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

  async function handleAttachmentFiles(selectedFiles: File[]) {
    setIsAttachmentMenuOpen(false)
    if (selectedFiles.length === 0) {
      return
    }

    const selectedAttachments = selectedFiles.map((file, index) => {
      const previewUrl = file.type.startsWith('image/') ? URL.createObjectURL(file) : null
      if (previewUrl) {
        attachmentPreviewUrlsRef.current.add(previewUrl)
      }
      return {
        localId: `${Date.now()}-${index}-${file.name}-${file.lastModified}`,
        file,
        previewUrl,
        status: 'UPLOADING' as const,
        progress: 0,
        fileId: null,
        errorMessage: null,
      }
    })
    setAttachments((current) => [...current, ...selectedAttachments])

    await Promise.all(selectedAttachments.map(async (attachment) => {
      if (attachment.file.size > MESSAGE_ATTACHMENT_MAX_SIZE_BYTES) {
        updateAttachment(attachment.localId, {
          status: 'FAILED',
          errorMessage: '파일 크기는 20MB까지 첨부할 수 있습니다.',
        })
        return
      }
      try {
        const presignedUpload = await createMessageAttachmentUploadUrl(workspaceId, {
          fileName: attachment.file.name,
          contentType: attachment.file.type || 'application/octet-stream',
          size: attachment.file.size,
        })
        await uploadMessageAttachmentToStorage(
          presignedUpload.uploadUrl,
          attachment.file,
          (progress) => updateAttachment(attachment.localId, { progress }),
        )
        updateAttachment(attachment.localId, {
          status: 'UPLOADED',
          progress: 100,
          fileId: presignedUpload.fileId,
        })
      } catch (error) {
        updateAttachment(attachment.localId, {
          status: 'FAILED',
          errorMessage: error instanceof ApiError ? error.message : '첨부 파일을 업로드하지 못했습니다.',
        })
      }
    }))
  }

  async function handleAttachmentSelection(event: ChangeEvent<HTMLInputElement>) {
    const selectedFiles = Array.from(event.target.files ?? [])
    event.target.value = ''
    await handleAttachmentFiles(selectedFiles)
  }

  function handleComposerDragOver(event: DragEvent<HTMLDivElement>) {
    if (!event.dataTransfer.types.includes('Files')) {
      return
    }
    event.preventDefault()
    if (readOnly || createMessage.isPending) {
      return
    }
    event.dataTransfer.dropEffect = 'copy'
    setIsComposerDragOver(true)
  }

  function handleComposerDragLeave(event: DragEvent<HTMLDivElement>) {
    if (event.currentTarget.contains(event.relatedTarget as Node)) {
      return
    }
    setIsComposerDragOver(false)
  }

  function handleComposerDrop(event: DragEvent<HTMLDivElement>) {
    if (!event.dataTransfer.types.includes('Files')) {
      return
    }
    event.preventDefault()
    setIsComposerDragOver(false)
    if (readOnly || createMessage.isPending) {
      return
    }
    void handleAttachmentFiles(Array.from(event.dataTransfer.files))
  }

  function updateAttachment(localId: string, patch: Partial<PendingMessageAttachment>) {
    setAttachments((current) => current.map((attachment) => (
      attachment.localId === localId ? { ...attachment, ...patch } : attachment
    )))
  }

  function removeAttachment(localId: string) {
    setAttachments((current) => {
      const attachment = current.find((item) => item.localId === localId)
      if (attachment?.previewUrl) {
        URL.revokeObjectURL(attachment.previewUrl)
        attachmentPreviewUrlsRef.current.delete(attachment.previewUrl)
      }
      return current.filter((attachment) => attachment.localId !== localId)
    })
  }

  function clearSelectedAttachments() {
    attachmentPreviewUrlsRef.current.forEach((previewUrl) => URL.revokeObjectURL(previewUrl))
    attachmentPreviewUrlsRef.current.clear()
    setAttachments([])
  }

  async function openMessageAttachment(messageId: string, fileId: string) {
    setMessageActionError(null)
    const downloadWindow = window.open('', '_blank')
    try {
      const { downloadUrl } = await createMessageAttachmentDownloadUrl(workspaceId, fileId)
      if (!downloadWindow) {
        throw new Error('첨부 파일 창을 열지 못했습니다.')
      }
      downloadWindow.opener = null
      downloadWindow.location.replace(downloadUrl)
    } catch (error) {
      downloadWindow?.close()
      setMessageActionError({
        messageId,
        message: error instanceof ApiError ? error.message : '첨부 파일을 열지 못했습니다.',
      })
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
    if ((!trimmedContent && message.attachments.length === 0) || updateMessage.isPending) {
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
        {messages.map((message, index) => {
          const isMine = message.author.memberId === currentMembershipId
          const previousMessage = messages[index - 1]
          const isContinuation = Boolean(
            previousMessage &&
            previousMessage.author.memberId === message.author.memberId &&
            isWithinMessageGroupInterval(previousMessage.createdAt, message.createdAt),
          )
          const showAuthor = !isMine && !isContinuation
          const isEditing = editingMessageId === message.id
          const canDeleteMessage = isMine || canManageMessages
          const isAttachmentOnly = !message.deleted && !message.content && message.attachments.length > 0
          return (
            <article
              key={message.id}
              className={`${styles.message} ${isMine ? styles.myMessage : ''} ${isContinuation ? styles.continuedMessage : ''} ${isAttachmentOnly ? styles.attachmentOnlyMessage : ''}`}
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
              {showAuthor && message.author.profileImageUrl ? (
                <img src={message.author.profileImageUrl} alt="" className={styles.avatar} />
              ) : showAuthor ? (
                <span className={styles.avatarFallback} aria-hidden="true">
                  {message.author.displayName.slice(0, 1)}
                </span>
              ) : null}
              <div className={styles.messageGroup}>
                {showAuthor && <strong className={styles.messageAuthor}>{message.author.displayName}</strong>}
                <div className={styles.messageBody}>
                  <div className={`${styles.messageContent} ${!message.deleted && !isEditing && !readOnly && canDeleteMessage ? styles.messageContentWithAction : ''}`}>
                    {message.edited && !message.deleted && (
                    <div className={styles.messageMeta}>
                      <span>수정됨</span>
                    </div>
                    )}
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
                      <button
                        type="submit"
                        disabled={updateMessage.isPending || (!editingContent.trim() && message.attachments.length === 0)}
                      >
                        {updateMessage.isPending ? '저장 중' : '저장'}
                      </button>
                    </div>
                  </form>
                ) : message.deleted ? (
                  <p className={message.deleted ? styles.deletedContent : undefined}>
                    삭제된 메시지입니다.
                  </p>
                ) : message.content ? <p>{message.content}</p> : null}
                  {!message.deleted && message.attachments.length > 0 && (
                    <ul className={styles.messageAttachments} aria-label="첨부 파일">
                      {message.attachments.map((attachment) => (
                        <MessageAttachmentItem
                          key={attachment.fileId}
                          workspaceId={workspaceId}
                          attachment={attachment}
                          onOpen={() => void openMessageAttachment(message.id, attachment.fileId)}
                        />
                      ))}
                    </ul>
                  )}
                  {!message.deleted && !isEditing && !readOnly && canDeleteMessage && (
                    <div
                      ref={openMenuId === message.id ? messageActionPopupRef : undefined}
                      className={styles.messageActionArea}
                    >
                      <button
                        type="button"
                        className={styles.messageActionTrigger}
                        aria-label="메시지 작업 메뉴"
                        aria-expanded={openMenuId === message.id}
                        aria-haspopup="menu"
                        onClick={() => setOpenMenuId((current) => current === message.id ? null : message.id)}
                      >
                        ⋯
                      </button>
                      {openMenuId === message.id && (
                        <div
                          className={styles.messageActionPopup}
                          role="menu"
                          aria-label="메시지 작업 메뉴"
                        >
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
              </div>
            </article>
          )
        })}
      </div>
      <form className={styles.composer} onSubmit={handleSubmit}>
        <label className={styles.composerLabel} htmlFor="channel-message-content">
          {readOnly ? '보관된 채널에서는 메시지를 작성할 수 없습니다.' : `${channelName}에 메시지 보내기`}
        </label>
        <div
          className={`${styles.composerSurface} ${isComposerDragOver ? styles.composerSurfaceDragOver : ''}`}
          onDragOver={handleComposerDragOver}
          onDragLeave={handleComposerDragLeave}
          onDrop={handleComposerDrop}
        >
          {attachments.length > 0 && (
            <ul className={styles.attachmentPreviewList} aria-label="선택한 첨부 파일">
              {attachments.map((attachment) => (
                <li
                key={attachment.localId}
                className={`${styles.attachmentPreviewItem} ${
                  attachment.previewUrl ? styles.imageAttachmentPreview : styles.fileAttachmentPreview
                }`}
              >
                {attachment.previewUrl ? (
                  <img src={attachment.previewUrl} alt={attachment.file.name} className={styles.attachmentThumbnail} />
                ) : (
                  <span className={styles.attachmentFileIcon} aria-hidden="true">파일</span>
                )}
                {!attachment.previewUrl && (
                  <div className={styles.attachmentPreviewInfo}>
                    <strong title={attachment.file.name}>{attachment.file.name}</strong>
                    <span>{formatFileSize(attachment.file.size)}</span>
                    {attachment.status === 'UPLOADING' && <span>업로드 중 {attachment.progress}%</span>}
                    {attachment.status === 'FAILED' && <span className={styles.attachmentError}>{attachment.errorMessage}</span>}
                  </div>
                )}
                {attachment.previewUrl && attachment.status === 'UPLOADING' && (
                  <span className={styles.attachmentPreviewStatus}>업로드 중 {attachment.progress}%</span>
                )}
                {attachment.previewUrl && attachment.status === 'FAILED' && (
                  <span className={`${styles.attachmentPreviewStatus} ${styles.attachmentError}`}>
                    업로드 실패
                  </span>
                )}
                <button
                  type="button"
                  className={styles.attachmentRemoveButton}
                  onClick={() => removeAttachment(attachment.localId)}
                  disabled={createMessage.isPending}
                  aria-label={`${attachment.file.name} 제거`}
                >
                  ×
                </button>
                </li>
              ))}
            </ul>
          )}
          <div className={styles.composerField}>
            <div ref={attachmentMenuRef} className={styles.attachmentMenuArea}>
            <input
              ref={attachmentInputRef}
              className={styles.attachmentInput}
              type="file"
              multiple
              onChange={(event) => void handleAttachmentSelection(event)}
              disabled={readOnly || createMessage.isPending}
            />
            <button
              type="button"
              className={styles.attachmentMenuTrigger}
              aria-label="첨부 메뉴"
              aria-expanded={isAttachmentMenuOpen}
              aria-haspopup="menu"
              onClick={() => setIsAttachmentMenuOpen((current) => !current)}
              disabled={readOnly || createMessage.isPending}
            >
              +
            </button>
            {isAttachmentMenuOpen && (
              <div className={styles.attachmentMenu} role="menu" aria-label="첨부 메뉴">
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => attachmentInputRef.current?.click()}
                >
                  파일 선택
                </button>
              </div>
            )}
            </div>
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
            <button
            type="submit"
            className={styles.composerSendButton}
            aria-label="메시지 전송"
            disabled={
              readOnly
              || createMessage.isPending
              || isAttachmentUploading
              || hasFailedAttachment
              || (!content.trim() && attachments.every((attachment) => attachment.status !== 'UPLOADED'))
            }
            >
              {createMessage.isPending ? '…' : '↑'}
            </button>
          </div>
        </div>
        {hasFailedAttachment && <p className={styles.submitError}>업로드에 실패한 첨부 파일을 제거해 주세요.</p>}
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

interface MessageAttachmentItemProps {
  workspaceId: string
  attachment: ChannelMessageAttachment
  onOpen: () => void
}

// 목록 조회 시에는 파일 메타데이터만 받고, 이미지에 한해 짧은 다운로드 URL을 추가로 발급받는다.
function MessageAttachmentItem({
  workspaceId,
  attachment,
  onOpen,
}: MessageAttachmentItemProps) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)

  useEffect(() => {
    if (!attachment.previewImage) {
      setPreviewUrl(null)
      return undefined
    }

    let active = true
    void createMessageAttachmentDownloadUrl(workspaceId, attachment.fileId)
      .then(({ downloadUrl }) => {
        if (active) {
          setPreviewUrl(downloadUrl)
        }
      })
      .catch(() => {
        if (active) {
          setPreviewUrl(null)
        }
      })
    return () => {
      active = false
    }
  }, [attachment.fileId, attachment.previewImage, workspaceId])

  if (attachment.previewImage) {
    return (
      <li className={styles.messageImageAttachment}>
        <button
          type="button"
          className={styles.messageImageAttachmentButton}
          onClick={onOpen}
          aria-label={`${attachment.fileName} 원본 열기`}
        >
          {previewUrl ? (
            <img src={previewUrl} alt={attachment.fileName} className={styles.messageImagePreview} />
          ) : (
            <span className={styles.messageImagePreviewPlaceholder}>이미지를 불러오는 중입니다.</span>
          )}
          <span className={styles.messageImageMeta}>
            <strong>{attachment.fileName}</strong>
            <small>{formatFileSize(attachment.size)}</small>
          </span>
        </button>
      </li>
    )
  }

  return (
    <li>
      <button
        type="button"
        className={styles.messageAttachmentButton}
        onClick={onOpen}
        aria-label={`${attachment.fileName} 열기`}
      >
        <span>파일</span>
        <strong>{attachment.fileName}</strong>
        <small>{formatFileSize(attachment.size)}</small>
      </button>
    </li>
  )
}

function formatMessageTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function isWithinMessageGroupInterval(firstValue: string, secondValue: string) {
  const firstDate = new Date(firstValue)
  const secondDate = new Date(secondValue)
  const isSameDate = firstDate.getFullYear() === secondDate.getFullYear()
    && firstDate.getMonth() === secondDate.getMonth()
    && firstDate.getDate() === secondDate.getDate()
  return isSameDate
    && secondDate.getTime() - firstDate.getTime() < MESSAGE_GROUP_INTERVAL_MILLISECONDS
}

function formatFileSize(size: number) {
  if (size < 1024 * 1024) {
    return `${Math.max(1, Math.ceil(size / 1024))}KB`
  }
  return `${(size / (1024 * 1024)).toFixed(1)}MB`
}
