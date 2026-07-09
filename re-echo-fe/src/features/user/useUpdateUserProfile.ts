import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authSessionQueryKey } from '../auth/authQuery'
import { updateUserProfile } from './userApi'

export function useUpdateUserProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateUserProfile,
    onSuccess: (user) => {
      queryClient.setQueryData(authSessionQueryKey, user)
    },
  })
}
