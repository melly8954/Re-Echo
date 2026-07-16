import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useCreateWorkspace } from './useCreateWorkspace'
import { workspaceListQueryKey } from './useWorkspaceList'
import { ApiError } from '../../shared/api/apiTypes'
import {
  createWorkspaceImageUploadUrl,
  updateWorkspace,
  uploadWorkspaceImageToStorage,
} from './workspaceApi'
import styles from './WorkspaceAccessDialog.module.css'

const workspaceImageMaxSizeBytes = 10 * 1024 * 1024
const allowedWorkspaceImageTypes = new Set([
  'image/jpeg',
  'image/png',
  'image/webp',
])

class WorkspaceImageUploadError extends Error {
  readonly workspaceId: string

  constructor(workspaceId: string) {
    super('워크스페이스 대표 이미지 업로드에 실패했습니다.')
    this.workspaceId = workspaceId
  }
}

interface ValidationErrorResult {
  fieldErrors?: Array<{
    field: string
    reason: string
  }>
}

interface WorkspaceCreateDialogProps {
  isOpen: boolean
  onClose: () => void
  onCreated: (workspaceId: string) => void
}

function getFieldError(error: unknown, field: string) {
  if (!(error instanceof ApiError)) {
    return null
  }

  const result = error.result as ValidationErrorResult | null
  return result?.fieldErrors?.find((fieldError) => fieldError.field === field)?.reason ?? null
}

// 어느 화면에서나 워크스페이스를 만들고 생성 직후 해당 홈으로 이동시킨다.
export function WorkspaceCreateDialog({
  isOpen,
  onClose,
  onCreated,
}: WorkspaceCreateDialogProps) {
  const queryClient = useQueryClient()
  const createWorkspace = useCreateWorkspace()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null)
  const [imageError, setImageError] = useState<string | null>(null)
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const [clientError, setClientError] = useState<string | null>(null)
  const nameError = clientError ?? getFieldError(createWorkspace.error, 'name')
  const isCreating = createWorkspace.isPending || isUploadingImage

  useEffect(() => {
    if (!selectedImage) {
      setImagePreviewUrl(null)
      return
    }

    const objectUrl = URL.createObjectURL(selectedImage)
    setImagePreviewUrl(objectUrl)
    return () => URL.revokeObjectURL(objectUrl)
  }, [selectedImage])

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const previousBodyOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isCreating) {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousBodyOverflow
    }
  }, [isCreating, isOpen, onClose])

  if (!isOpen) {
    return null
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedName = name.trim()

    setClientError(null)
    createWorkspace.reset()
    if (!trimmedName) {
      setClientError('워크스페이스 이름을 입력해 주세요.')
      return
    }

    try {
      const workspace = await createWorkspace.mutateAsync({
        name: trimmedName,
        description: description.trim() || null,
      })
      if (selectedImage) {
        await uploadAndConnectWorkspaceImage(workspace.id, selectedImage)
      }
      await queryClient.invalidateQueries({ queryKey: workspaceListQueryKey })
      onCreated(workspace.id)
    } catch (error) {
      if (error instanceof WorkspaceImageUploadError) {
        await queryClient.invalidateQueries({ queryKey: workspaceListQueryKey })
        onCreated(error.workspaceId)
      }
    }
  }

  function handleImageChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null
    setImageError(null)
    if (!file) {
      return
    }
    if (!allowedWorkspaceImageTypes.has(file.type)) {
      setImageError('JPG, PNG, WebP 이미지만 업로드할 수 있습니다.')
      event.target.value = ''
      return
    }
    if (file.size > workspaceImageMaxSizeBytes) {
      setImageError('대표 이미지는 10MB 이하로 업로드해 주세요.')
      event.target.value = ''
      return
    }
    setSelectedImage(file)
  }

  async function uploadAndConnectWorkspaceImage(workspaceId: string, file: File) {
    try {
      setIsUploadingImage(true)
      const presignedUpload = await createWorkspaceImageUploadUrl(workspaceId, {
        fileName: file.name,
        contentType: file.type,
        size: file.size,
      })
      await uploadWorkspaceImageToStorage(presignedUpload.uploadUrl, file)
      await updateWorkspace(workspaceId, {
        name: name.trim(),
        description: description.trim() || null,
        imageFileId: presignedUpload.fileId,
      })
    } catch {
      throw new WorkspaceImageUploadError(workspaceId)
    } finally {
      setIsUploadingImage(false)
    }
  }

  return (
    <div className={styles.layer}>
      <button className={styles.overlay} type="button" aria-label="워크스페이스 생성 닫기" onClick={onClose} />
      <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="workspace-create-title">
        <header>
          <div>
            <p>새 워크스페이스</p>
            <h2 id="workspace-create-title">워크스페이스 만들기</h2>
          </div>
          <button type="button" onClick={onClose} disabled={isCreating}>닫기</button>
        </header>
        <form onSubmit={(event) => void handleSubmit(event)}>
          <label>
            <span>워크스페이스 이름</span>
            <input
              value={name}
              maxLength={100}
              autoFocus
              aria-invalid={Boolean(nameError)}
              aria-describedby={nameError ? 'workspace-name-error' : undefined}
              onChange={(event) => {
                setName(event.target.value)
                setClientError(null)
                createWorkspace.reset()
              }}
            />
            {nameError && <small id="workspace-name-error" className={styles.error} role="alert">{nameError}</small>}
          </label>
          <label>
            <span>설명</span>
            <textarea value={description} maxLength={500} rows={3} onChange={(event) => setDescription(event.target.value)} />
          </label>
          <div className={styles.imageSection}>
            {imagePreviewUrl ? (
              <img src={imagePreviewUrl} alt="선택한 워크스페이스 대표 이미지 미리보기" />
            ) : (
              <span className={styles.imageFallback} aria-hidden="true">
                {name.trim().slice(0, 1) || 'W'}
              </span>
            )}
            <div className={styles.imageControls}>
              <strong>대표 이미지 <small>선택</small></strong>
              <p>JPG, PNG, WebP 형식의 10MB 이하 이미지를 사용할 수 있습니다.</p>
              <label className={styles.imageSelectButton} htmlFor="workspace-create-image">
                이미지 선택
              </label>
              <input
                id="workspace-create-image"
                className={styles.imageInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleImageChange}
              />
              {imageError && <small className={styles.error} role="alert">{imageError}</small>}
            </div>
          </div>
          {createWorkspace.isError && !nameError && (
            <p className={styles.error} role="alert">
              {createWorkspace.error instanceof ApiError ? createWorkspace.error.message : '워크스페이스를 만들지 못했습니다.'}
            </p>
          )}
          <footer>
            <button type="button" onClick={onClose} disabled={isCreating}>취소</button>
            <button type="submit" disabled={isCreating}>
              {isCreating ? '생성 중' : '워크스페이스 만들기'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}
