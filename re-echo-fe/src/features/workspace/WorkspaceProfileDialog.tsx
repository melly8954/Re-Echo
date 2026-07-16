import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../shared/api/apiTypes'
import { workspaceDetailQueryKey } from './useWorkspaceDetail'
import { workspaceMembersQueryKey } from './useWorkspaceMembers'
import {
  createWorkspaceProfileImageUploadUrl,
  updateWorkspaceProfile,
  uploadWorkspaceProfileImageToStorage,
  type WorkspaceDetail,
  type WorkspaceMemberList,
} from './workspaceApi'
import styles from './WorkspaceSettingsDialog.module.css'

const profileImageMaxSizeBytes = 10 * 1024 * 1024
const allowedProfileImageTypes = new Set([
  'image/jpeg',
  'image/png',
  'image/webp',
])

interface WorkspaceProfileDialogProps {
  workspace: WorkspaceDetail
  isOpen: boolean
  onClose: () => void
  embedded?: boolean
}

// 계정 기본 프로필과 분리된 워크스페이스별 프로필 수정 흐름을 제공한다.
export function WorkspaceProfileDialog({
  workspace,
  isOpen,
  onClose,
  embedded = false,
}: WorkspaceProfileDialogProps) {
  const queryClient = useQueryClient()
  const updateProfileMutation = useMutation({
    mutationFn: ({ request }: { request: Parameters<typeof updateWorkspaceProfile>[1] }) =>
      updateWorkspaceProfile(workspace.id, request),
  })
  const { reset: resetWorkspaceProfile } = updateProfileMutation
  const [displayName, setDisplayName] = useState(workspace.myMembership.displayName)
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null)
  const [shouldRemoveImage, setShouldRemoveImage] = useState(false)
  const [imageError, setImageError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const trimmedDisplayName = displayName.trim()
  const currentImageUrl = imagePreviewUrl ?? (
    shouldRemoveImage ? null : workspace.myMembership.profileImageUrl
  )
  const isSaving = updateProfileMutation.isPending || isUploadingImage
  const isUnchanged =
    trimmedDisplayName === workspace.myMembership.displayName &&
    !selectedImage &&
    !shouldRemoveImage

  useEffect(() => {
    if (!isOpen) {
      return
    }

    setDisplayName(workspace.myMembership.displayName)
    setSelectedImage(null)
    setShouldRemoveImage(false)
    setImageError(null)
    setSubmitError(null)
    resetWorkspaceProfile()
  }, [isOpen, resetWorkspaceProfile, workspace])

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
    if (!isOpen || embedded) {
      return undefined
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSaving) {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [embedded, isOpen, isSaving, onClose])

  if (!isOpen) {
    return null
  }

  function handleImageChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null
    setImageError(null)
    setSubmitError(null)
    updateProfileMutation.reset()

    if (!file) {
      return
    }
    if (!allowedProfileImageTypes.has(file.type)) {
      setImageError('JPG, PNG, WebP 이미지만 업로드할 수 있습니다.')
      event.target.value = ''
      return
    }
    if (file.size > profileImageMaxSizeBytes) {
      setImageError('프로필 이미지는 10MB 이하로 업로드해 주세요.')
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
    updateProfileMutation.reset()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitError(null)

    if (!trimmedDisplayName) {
      setSubmitError('표시 이름을 입력해 주세요.')
      return
    }

    try {
      setIsUploadingImage(true)
      const profileImageFileId = selectedImage
        ? await uploadSelectedImage(selectedImage)
        : undefined
      const updatedMembership = await updateProfileMutation.mutateAsync({
        request: {
          displayName: trimmedDisplayName,
          ...(selectedImage ? { profileImageFileId } : {}),
          ...(shouldRemoveImage ? { profileImageFileId: null } : {}),
        },
      })
      queryClient.setQueryData<WorkspaceDetail>(
        workspaceDetailQueryKey(workspace.id),
        (currentWorkspace) => currentWorkspace && ({
          ...currentWorkspace,
          myMembership: updatedMembership,
        }),
      )
      queryClient.setQueryData<WorkspaceMemberList>(
        workspaceMembersQueryKey(workspace.id),
        (currentMembers) => currentMembers && ({
          ...currentMembers,
          contents: currentMembers.contents.map((member) => (
            member.id === updatedMembership.id ? updatedMembership : member
          )),
        }),
      )
      if (!embedded) {
        onClose()
      }
    } catch (error) {
      setSubmitError(
        error instanceof ApiError
          ? error.message
          : '워크스페이스 프로필을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      )
    } finally {
      setIsUploadingImage(false)
    }
  }

  async function uploadSelectedImage(file: File) {
    const presignedUpload = await createWorkspaceProfileImageUploadUrl(workspace.id, {
      fileName: file.name,
      contentType: file.type,
      size: file.size,
    })
    await uploadWorkspaceProfileImageToStorage(presignedUpload.uploadUrl, file)
    return presignedUpload.fileId
  }

  const dialogContent = (
      <section
        className={embedded ? `${styles.dialog} ${styles.embeddedDialog}` : styles.dialog}
        role={embedded ? undefined : 'dialog'}
        aria-modal={embedded ? undefined : true}
        aria-labelledby="workspace-profile-title"
      >
        <header className={styles.header}>
          <div>
            <p>워크스페이스별 프로필</p>
            <h2 id="workspace-profile-title">{workspace.name} 프로필</h2>
          </div>
          {!embedded && (
            <button type="button" onClick={onClose} disabled={isSaving}>
              닫기
            </button>
          )}
        </header>
        <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
          <div className={styles.imageSection}>
            {currentImageUrl ? (
              <img src={currentImageUrl} alt={`현재 ${workspace.name} 프로필 이미지`} />
            ) : (
              <span className={styles.imageFallback} aria-hidden="true">
                {workspace.myMembership.displayName.slice(0, 1)}
              </span>
            )}
            <div className={styles.imageControls}>
              <strong>프로필 이미지</strong>
              <p>JPG, PNG, WebP 형식의 10MB 이하 이미지를 사용할 수 있습니다.</p>
              <div className={styles.imageActions}>
                <label htmlFor="workspace-profile-image">이미지 선택</label>
                <input
                  id="workspace-profile-image"
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
            <span>표시 이름</span>
            <input
              type="text"
              value={displayName}
              maxLength={80}
              onChange={(event) => {
                setDisplayName(event.target.value)
                setSubmitError(null)
                updateProfileMutation.reset()
              }}
            />
            <small className={styles.help}>
              {workspace.name}에만 표시되며 계정 기본 프로필은 바뀌지 않습니다.
            </small>
          </label>
          {submitError && <p className={styles.error} role="alert">{submitError}</p>}
          {embedded && updateProfileMutation.isSuccess && (
            <p className={styles.success} role="status">
              워크스페이스 프로필을 수정했습니다.
            </p>
          )}
          <footer className={styles.footer}>
            {!embedded && (
              <button type="button" onClick={onClose} disabled={isSaving}>
                취소
              </button>
            )}
            <button type="submit" disabled={isSaving || isUnchanged}>
              {isSaving ? '저장 중...' : '변경 사항 저장'}
            </button>
          </footer>
        </form>
      </section>
  )

  if (embedded) {
    return dialogContent
  }

  return (
    <div className={styles.layer}>
      <button
        className={styles.overlay}
        type="button"
        aria-label="내 프로필 닫기"
        onClick={onClose}
        disabled={isSaving}
      />
      {dialogContent}
    </div>
  )
}
