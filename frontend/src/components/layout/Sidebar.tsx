import { Link, useLocation } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { useAuth } from '@/store/auth'
import {
  LayoutDashboard,
  ShoppingCart,
  Settings,
  LogOut,
  Package,
} from 'lucide-react'
import { Button } from '@/components/ui/button'

const navItems = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/orders', label: 'Orders', icon: ShoppingCart },
  { path: '/settings', label: 'Settings', icon: Settings, roles: ['SUPER_ADMIN'] },
]

export function Sidebar() {
  const location = useLocation()
  const { user, logout } = useAuth()

  return (
    <aside className="flex h-screen w-64 flex-col border-r bg-card">
      <div className="flex h-16 items-center gap-2 border-b px-6">
        <Package className="h-6 w-6 text-primary" />
        <span className="text-lg font-bold">Palmonas CRM</span>
      </div>

      <nav className="flex-1 space-y-1 p-4">
        {navItems
          .filter((item) => !item.roles || (user && item.roles.includes(user.role)))
          .map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.path ||
              (item.path !== '/' && location.pathname.startsWith(item.path))
            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                )}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            )
          })}
      </nav>

      <div className="border-t p-4">
        <div className="mb-3 px-3">
          <p className="text-sm font-medium">{user?.fullName}</p>
          <p className="text-xs text-muted-foreground">{user?.role}</p>
        </div>
        <Button variant="ghost" className="w-full justify-start gap-3" onClick={logout}>
          <LogOut className="h-4 w-4" />
          Logout
        </Button>
      </div>
    </aside>
  )
}
