import { QueryClient } from '@tanstack/react-query'

// 서버 상태의 재시도와 화면 복귀 시 갱신 정책을 공통으로 관리한다.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})
