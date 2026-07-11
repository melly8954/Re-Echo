import { useMutation } from '@tanstack/react-query'
import { createWorkspace } from './workspaceApi'

export function useCreateWorkspace() {
  return useMutation({
    mutationFn: createWorkspace,
  })
}
