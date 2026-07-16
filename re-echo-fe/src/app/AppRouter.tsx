import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { LoginPage } from '../pages/LoginPage'
import { OAuthCallbackPage } from '../pages/OAuthCallbackPage'
import { InviteLinkPage } from '../pages/InviteLinkPage'
import { ProfileSettingsPage } from '../pages/ProfileSettingsPage'
import { WorkspaceHomePage } from '../pages/WorkspaceHomePage'
import { WorkspacePage } from '../pages/WorkspacePage'
import { WorkspaceStartPage } from '../pages/WorkspaceStartPage'

// 공개 경로와 인증이 필요한 앱 경로를 한곳에서 구성한다.
export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      <Route path="/invite-links/:token" element={<InviteLinkPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<WorkspaceStartPage />} />
        <Route path="/workspaces/:workspaceId" element={<WorkspaceHomePage />} />
        <Route
          path="/workspaces/:workspaceId/channels/:channelId"
          element={<WorkspacePage />}
        />
        <Route path="/settings/profile" element={<ProfileSettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
