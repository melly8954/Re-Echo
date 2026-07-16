import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authSessionQueryKey } from '../auth/authQuery'
import { updateUserProfile } from './userApi'

// 프로필 변경 성공 후 인증 Context가 참조하는 현재 사용자 정보를 갱신한다.
export function useUpdateUserProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateUserProfile,
    onSuccess: (user) => {
      queryClient.setQueryData(authSessionQueryKey, user)
    },
  })
}
