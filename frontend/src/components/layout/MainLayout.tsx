import { Outlet, Navigate } from 'react-router-dom'
import { useAuth } from '@/store/auth'
import { Sidebar } from './Sidebar'

export function MainLayout() {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar />
      <main className="flex-1 overflow-y-auto bg-muted/30 p-8">
        <Outlet />
      </main>
    </div>
  )
}
