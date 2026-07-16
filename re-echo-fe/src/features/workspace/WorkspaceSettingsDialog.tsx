import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../shared/api/apiTypes'
import {
  createWorkspaceImageUploadUrl,
  updateWorkspace,
  uploadWorkspaceImageToStorage,
  type WorkspaceDetail,
} from './workspaceApi'
import { workspaceDetailQueryKey } from './useWorkspaceDetail'
import { workspaceListQueryKey } from './useWorkspaceList'
import styles from './WorkspaceSettingsDialog.module.css'

const workspaceImageMaxSizeBytes = 10 * 1024 * 1024
const allowedWorkspaceImageTypes = new Set([
  'image/jpeg',
  'image/png',
  'image/webp',
])

interface WorkspaceSettingsDialogProps {
  workspace: WorkspaceDetail
  isOpen: boolean
  onClose: () => void
  onRequestArchive?: () => void
}

// 대표 이미지와 기본 정보를 한 번에 바꿔 변경 흐름을 분산하지 않는다.
export function WorkspaceSettingsDialog({
  workspace,
  isOpen,
  onClose,
  onRequestArchive,
}: WorkspaceSettingsDialogProps) {
  const queryClient = useQueryClient()
  const updateWorkspaceMutation = useMutation({
    mutationFn: ({ request }: { request: Parameters<typeof updateWorkspace>[1] }) =>
      updateWorkspace(workspace.id, request),
  })
  const { reset: resetWorkspaceUpdate } = updateWorkspaceMutation
  const [name, setName] = useState(workspace.name)
  const [description, setDescription] = useState(workspace.description ?? '')
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null)
  const [shouldRemoveImage, setShouldRemoveImage] = useState(false)
  const [imageError, setImageError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const isSaving = updateWorkspaceMutation.isPending || isUploadingImage
  const canUpdateName = workspace.myMembership.role === 'OWNER'

  useEffect(() => {
    if (!isOpen) {
      return
    }

    setName(workspace.name)
    setDescription(workspace.description ?? '')
    setSelectedImage(null)
    setShouldRemoveImage(false)
    setImageError(null)
    setSubmitError(null)
    resetWorkspaceUpdate()
  }, [isOpen, resetWorkspaceUpdate, workspace])

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

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isSaving, onClose])

  if (!isOpen) {
    return null
  }

  const trimmedName = name.trim()
  const normalizedDescription = description.trim() || null
  const currentImageUrl = imagePreviewUrl ?? (shouldRemoveImage ? null : workspace.imageUrl)
  const currentImageFileId = shouldRemoveImage
    ? null
    : workspace.imageFileId
  const isUnchanged =
    trimmedName === workspace.name &&
    normalizedDescription === workspace.description &&
    !selectedImage &&
    !shouldRemoveImage

  function handleImageChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null
    setImageError(null)
    setSubmitError(null)
    updateWorkspaceMutation.reset()

    if (!file) {
      return
    }
    if (!allowedWorkspaceImageTypes.has(file.type)) {
      setImageError('JPG, PNG, WebP 이미지만 업로드할 수 있습니다.')
      event.target.value = ''
      return
    }
    if (file.size > workspaceImageMaxSizeBytes) {
      setImageError('워크스페이스 대표 이미지는 10MB 이하로 업로드해 주세요.')
      event.target.value = ''
      return
    }

    setShouldRemoveImage(false)
    setSelectedImage(file)
  }

  function handleRemoveImage() {
    setSelectedImage(null)
    setShouldRemoveImage(true)
    setImageError(null)
    setSubmitError(null)
    updateWorkspaceMutation.reset()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitError(null)

    if (!trimmedName) {
      setSubmitError('워크스페이스 이름을 입력해 주세요.')
      return
    }

    try {
      setIsUploadingImage(true)
      const imageFileId = selectedImage
        ? await uploadSelectedImage(selectedImage)
        : currentImageFileId
      const updatedWorkspace = await updateWorkspaceMutation.mutateAsync({
        request: {
          name: trimmedName,
          description: normalizedDescription,
          imageFileId,
        },
      })
      queryClient.setQueryData(workspaceDetailQueryKey(workspace.id), updatedWorkspace)
      await queryClient.invalidateQueries({ queryKey: workspaceListQueryKey })
      onClose()
    } catch (error) {
      setSubmitError(
        error instanceof ApiError
          ? error.message
          : '워크스페이스 설정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      )
    } finally {
      setIsUploadingImage(false)
    }
  }

  async function uploadSelectedImage(file: File) {
    const presignedUpload = await createWorkspaceImageUploadUrl(workspace.id, {
      fileName: file.name,
      contentType: file.type,
      size: file.size,
    })
    await uploadWorkspaceImageToStorage(presignedUpload.uploadUrl, file)
    return presignedUpload.fileId
  }

  return (
    <div className={styles.layer}>
      <button
        className={styles.overlay}
        type="button"
        aria-label="워크스페이스 설정 닫기"
        onClick={onClose}
        disabled={isSaving}
      />
      <section
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby="workspace-settings-title"
      >
        <header className={styles.header}>
          <div>
            <p>워크스페이스 설정</p>
            <h2 id="workspace-settings-title">기본 정보</h2>
          </div>
          <button type="button" onClick={onClose} disabled={isSaving}>
            닫기
          </button>
        </header>
        <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
          <div className={styles.imageSection}>
            {currentImageUrl ? (
              <img src={currentImageUrl} alt="현재 워크스페이스 대표 이미지" />
            ) : (
              <span className={styles.imageFallback} aria-hidden="true">
                {workspace.name.slice(0, 1)}
              </span>
            )}
            <div className={styles.imageControls}>
              <strong>대표 이미지</strong>
              <p>JPG, PNG, WebP 형식의 10MB 이하 이미지를 사용할 수 있습니다.</p>
              <div className={styles.imageActions}>
                <label htmlFor="workspace-image">이미지 선택</label>
                <input
                  id="workspace-image"
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={handleImageChange}
                />
                {(currentImageUrl || selectedImage) && (
                  <button type="button" onClick={handleRemoveImage}>
                    이미지 제거
                  </button>
                )}
              </div>
              {imageError && <p className={styles.error} role="alert">{imageError}</p>}
            </div>
          </div>
          <label className={styles.field}>
            <span>워크스페이스 이름</span>
            <input
              type="text"
              value={name}
              maxLength={100}
              readOnly={!canUpdateName}
              aria-describedby={canUpdateName ? undefined : 'workspace-name-readonly-help'}
              onChange={(event) => {
                setName(event.target.value)
                setSubmitError(null)
              }}
            />
            {!canUpdateName && (
              <small id="workspace-name-readonly-help" className={styles.help}>
                워크스페이스 이름은 소유자만 변경할 수 있습니다.
              </small>
            )}
          </label>
          <label className={styles.field}>
            <span>설명</span>
            <textarea
              value={description}
              maxLength={500}
              rows={4}
              onChange={(event) => {
                setDescription(event.target.value)
                setSubmitError(null)
              }}
            />
          </label>
          {submitError && <p className={styles.error} role="alert">{submitError}</p>}
          {onRequestArchive && (
            <section className={styles.archiveSection} aria-labelledby="workspace-archive-title">
              <div>
                <strong id="workspace-archive-title">워크스페이스 보관</strong>
                <p>활성 채널이 읽기 전용으로 전환되며, 15일 안에만 복원할 수 있습니다.</p>
              </div>
              <button type="button" onClick={onRequestArchive} disabled={isSaving}>
                워크스페이스 보관
              </button>
            </section>
          )}
          <footer className={styles.footer}>
            <button type="button" onClick={onClose} disabled={isSaving}>
              취소
            </button>
            <button type="submit" disabled={isSaving || isUnchanged}>
              {isSaving ? '저장 중...' : '변경 사항 저장'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}
