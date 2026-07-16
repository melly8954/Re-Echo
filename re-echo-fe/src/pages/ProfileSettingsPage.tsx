import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { useAuth } from '../features/auth/useAuth'
import { WorkspaceProfileDialog } from '../features/workspace/WorkspaceProfileDialog'
import { useWorkspaceDetail } from '../features/workspace/useWorkspaceDetail'
import {
  createProfileImageUploadUrl,
  uploadProfileImageToStorage,
} from '../features/user/userApi'
import { useUpdateUserProfile } from '../features/user/useUpdateUserProfile'
import { ApiError } from '../shared/api/apiTypes'
import styles from './ProfileSettingsPage.module.css'

const profileImageMaxSizeBytes = 10 * 1024 * 1024
const allowedProfileImageTypes = new Set([
  'image/jpeg',
  'image/png',
  'image/webp',
])

interface ValidationErrorResult {
  fieldErrors?: Array<{
    field: string
    reason: string
  }>
}

function getDisplayNameError(error: unknown) {
  if (!(error instanceof ApiError)) {
    return null
  }

  const result = error.result as ValidationErrorResult | null
  return (
    result?.fieldErrors?.find((fieldError) => fieldError.field === 'displayName')
      ?.reason ?? null
  )
}

export function ProfileSettingsPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const workspaceId = searchParams.get('workspaceId') ?? ''
  const workspaceQuery = useWorkspaceDetail(workspaceId)
  const isWorkspaceProfileScope = searchParams.get('scope') === 'workspace' && Boolean(workspaceId)
  const updateProfile = useUpdateUserProfile()
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [clientError, setClientError] = useState<string | null>(null)
  const [profileImageError, setProfileImageError] = useState<string | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [selectedProfileImage, setSelectedProfileImage] = useState<File | null>(null)
  const [profileImagePreviewUrl, setProfileImagePreviewUrl] = useState<string | null>(null)
  const [shouldRemoveProfileImage, setShouldRemoveProfileImage] = useState(false)
  const [isUploadingProfileImage, setIsUploadingProfileImage] = useState(false)
  const trimmedDisplayName = displayName.trim()
  const isUnchanged =
    trimmedDisplayName === user?.displayName &&
    !selectedProfileImage &&
    !shouldRemoveProfileImage
  const currentProfileImageUrl =
    profileImagePreviewUrl ??
    (shouldRemoveProfileImage ? null : user?.profileImageUrl ?? null)
  const isSaving = updateProfile.isPending || isUploadingProfileImage

  useEffect(() => {
    if (!selectedProfileImage) {
      setProfileImagePreviewUrl(null)
      return
    }

    const objectUrl = URL.createObjectURL(selectedProfileImage)
    setProfileImagePreviewUrl(objectUrl)

    return () => {
      URL.revokeObjectURL(objectUrl)
    }
  }, [selectedProfileImage])

  function handleProfileImageChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null
    setProfileImageError(null)
    setSubmitError(null)
    updateProfile.reset()

    if (!file) {
      return
    }

    if (!allowedProfileImageTypes.has(file.type)) {
      setProfileImageError('JPG, PNG, WebP 이미지만 업로드할 수 있습니다.')
      event.target.value = ''
      return
    }

    if (file.size > profileImageMaxSizeBytes) {
      setProfileImageError('프로필 이미지는 10MB 이하로 업로드해 주세요.')
      event.target.value = ''
      return
    }

    setShouldRemoveProfileImage(false)
    setSelectedProfileImage(file)
  }

  function handleRemoveProfileImage() {
    setSelectedProfileImage(null)
    setProfileImageError(null)
    setSubmitError(null)
    setShouldRemoveProfileImage(true)
    updateProfile.reset()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClientError(null)
    setSubmitError(null)

    if (!trimmedDisplayName) {
      setClientError('표시 이름을 입력해 주세요.')
      return
    }

    if (trimmedDisplayName.length > 80) {
      setClientError('표시 이름은 80자 이하로 입력해 주세요.')
      return
    }

    try {
      setIsUploadingProfileImage(true)
      const profileImageFileId = selectedProfileImage
        ? await uploadSelectedProfileImage(selectedProfileImage)
        : undefined

      await updateProfile.mutateAsync({
        displayName: trimmedDisplayName,
        ...(selectedProfileImage ? { profileImageFileId } : {}),
        ...(shouldRemoveProfileImage ? { profileImageFileId: null } : {}),
      })
      setSelectedProfileImage(null)
      setShouldRemoveProfileImage(false)
    } catch {
      setSubmitError('프로필을 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.')
      // mutation 상태를 통해 필드 오류 또는 공통 오류를 화면에 표시한다.
    } finally {
      setIsUploadingProfileImage(false)
    }
  }

  async function uploadSelectedProfileImage(file: File) {
    const presignedUpload = await createProfileImageUploadUrl({
      fileName: file.name,
      contentType: file.type,
      size: file.size,
    })
    await uploadProfileImageToStorage(presignedUpload.uploadUrl, file)
    return presignedUpload.fileId
  }

  const fieldError = clientError ?? getDisplayNameError(updateProfile.error)

  function selectProfileScope(scope: 'global' | 'workspace') {
    const nextSearchParams = new URLSearchParams(searchParams)
    nextSearchParams.set('scope', scope)
    setSearchParams(nextSearchParams)
  }

  return (
    <AppShell>
      <section className={styles.page} aria-labelledby="profile-title">
        <div className={styles.heading}>
          <p>계정 설정</p>
          <h1 id="profile-title">프로필</h1>
          <span>
            전체 기본 프로필과 현재 워크스페이스 프로필을 구분해 관리합니다.
          </span>
        </div>

        <div className={styles.scopeTabs} role="tablist" aria-label="프로필 범위">
          <button
            type="button"
            role="tab"
            aria-selected={!isWorkspaceProfileScope}
            className={!isWorkspaceProfileScope ? styles.scopeTabActive : styles.scopeTab}
            onClick={() => selectProfileScope('global')}
          >
            전체 프로필
          </button>
          {workspaceId && (
            <button
              type="button"
              role="tab"
              aria-selected={isWorkspaceProfileScope}
              className={isWorkspaceProfileScope ? styles.scopeTabActive : styles.scopeTab}
              onClick={() => selectProfileScope('workspace')}
            >
              이 워크스페이스 프로필
            </button>
          )}
        </div>

        {isWorkspaceProfileScope ? (
          <section className={styles.workspaceScopePanel} aria-label="워크스페이스 프로필 설정">
            {workspaceQuery.isLoading && <p>워크스페이스 프로필을 불러오는 중입니다.</p>}
            {workspaceQuery.isError && (
              <p className={styles.formError} role="alert">
                워크스페이스 프로필을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
              </p>
            )}
            {workspaceQuery.data && (
              <WorkspaceProfileDialog
                workspace={workspaceQuery.data}
                isOpen
                embedded
                onClose={() => undefined}
              />
            )}
          </section>
        ) : (
          <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
          <div className={styles.profileImageSection}>
            {currentProfileImageUrl ? (
              <img src={currentProfileImageUrl} alt="현재 프로필" />
            ) : (
              <span className={styles.avatarFallback} aria-hidden="true">
                {user?.displayName.slice(0, 1) ?? 'R'}
              </span>
            )}
            <div className={styles.profileImageControls}>
              <strong>프로필 이미지</strong>
              <p>JPG, PNG, WebP 형식의 10MB 이하 이미지를 사용할 수 있습니다.</p>
              <div className={styles.profileImageActions}>
                <label htmlFor="profile-image">이미지 선택</label>
                <input
                  id="profile-image"
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={handleProfileImageChange}
                />
                {(currentProfileImageUrl || selectedProfileImage) && (
                  <button type="button" onClick={handleRemoveProfileImage}>
                    이미지 제거
                  </button>
                )}
              </div>
              {profileImageError && (
                <p className={styles.fieldError} role="alert">
                  {profileImageError}
                </p>
              )}
            </div>
          </div>

          <div className={styles.field}>
            <label htmlFor="display-name">표시 이름</label>
            <input
              id="display-name"
              name="displayName"
              type="text"
              value={displayName}
              maxLength={80}
              aria-describedby={fieldError ? 'display-name-error' : 'display-name-help'}
              aria-invalid={Boolean(fieldError)}
              onChange={(event) => {
                setDisplayName(event.target.value)
                setClientError(null)
                updateProfile.reset()
              }}
            />
            {fieldError ? (
              <p className={styles.fieldError} id="display-name-error" role="alert">
                {fieldError}
              </p>
            ) : (
              <p className={styles.help} id="display-name-help">
                공백을 제외하고 1자 이상 80자 이하로 입력해 주세요.
              </p>
            )}
          </div>

          {updateProfile.isError && !fieldError && (
            <p className={styles.formError} role="alert">
              {submitError ?? '프로필을 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.'}
            </p>
          )}

          {submitError && !updateProfile.isError && (
            <p className={styles.formError} role="alert">
              {submitError}
            </p>
          )}

          {updateProfile.isSuccess && (
            <p className={styles.success} role="status">
              기본 프로필을 수정했습니다.
            </p>
          )}

          <div className={styles.actions}>
            <button
              type="submit"
              disabled={isSaving || isUnchanged}
            >
              {isSaving ? '저장 중...' : '변경 사항 저장'}
            </button>
          </div>
          </form>
        )}
      </section>
    </AppShell>
  )
}
