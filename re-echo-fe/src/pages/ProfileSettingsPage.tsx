import { useState, type FormEvent } from 'react'
import { AppShell } from '../components/layout/AppShell'
import { useAuth } from '../features/auth/useAuth'
import { useUpdateUserProfile } from '../features/user/useUpdateUserProfile'
import { ApiError } from '../shared/api/apiTypes'
import styles from './ProfileSettingsPage.module.css'

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
  const updateProfile = useUpdateUserProfile()
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [clientError, setClientError] = useState<string | null>(null)
  const trimmedDisplayName = displayName.trim()
  const isUnchanged = trimmedDisplayName === user?.displayName

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setClientError(null)

    if (!trimmedDisplayName) {
      setClientError('표시 이름을 입력해 주세요.')
      return
    }

    if (trimmedDisplayName.length > 80) {
      setClientError('표시 이름은 80자 이하로 입력해 주세요.')
      return
    }

    try {
      await updateProfile.mutateAsync({
        displayName: trimmedDisplayName,
        profileImageUrl: user?.profileImageUrl ?? null,
      })
    } catch {
      // mutation 상태를 통해 필드 오류 또는 공통 오류를 화면에 표시한다.
    }
  }

  const fieldError = clientError ?? getDisplayNameError(updateProfile.error)

  return (
    <AppShell>
      <section className={styles.page} aria-labelledby="profile-title">
        <div className={styles.heading}>
          <p>계정 설정</p>
          <h1 id="profile-title">기본 프로필</h1>
          <span>
            새 워크스페이스에 참여할 때 사용할 기본 정보를 관리합니다.
          </span>
        </div>

        <form className={styles.form} onSubmit={(event) => void handleSubmit(event)}>
          <div className={styles.profileImageSection}>
            {user?.profileImageUrl ? (
              <img src={user.profileImageUrl} alt="현재 프로필" />
            ) : (
              <span className={styles.avatarFallback} aria-hidden="true">
                {user?.displayName.slice(0, 1) ?? 'R'}
              </span>
            )}
            <div>
              <strong>프로필 이미지</strong>
              <p>파일 업로드 기능이 준비되면 이곳에서 변경할 수 있습니다.</p>
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
              프로필을 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.
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
              disabled={updateProfile.isPending || isUnchanged}
            >
              {updateProfile.isPending ? '저장 중...' : '변경 사항 저장'}
            </button>
          </div>
        </form>
      </section>
    </AppShell>
  )
}
