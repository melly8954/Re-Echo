import { QueryClientProvider } from '@tanstack/react-query'
import { HomePage } from './pages/HomePage'
import { queryClient } from './shared/query/queryClient'

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <HomePage />
    </QueryClientProvider>
  )
}

export default App
