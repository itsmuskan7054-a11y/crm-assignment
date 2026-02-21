import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import type { UserInfo } from '@/types/api'
import { authApi } from '@/services/api'

interface AuthState {
  user: UserInfo | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const isAuthenticated = !!user && !!localStorage.getItem('accessToken')

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await authApi.login(email, password)
    const tokenData = data.data
    localStorage.setItem('accessToken', tokenData.accessToken)
    localStorage.setItem('refreshToken', tokenData.refreshToken)
    localStorage.setItem('user', JSON.stringify(tokenData.user))
    setUser(tokenData.user)
  }, [])

  const register = useCallback(async (email: string, password: string, fullName: string) => {
    const { data } = await authApi.register(email, password, fullName)
    const tokenData = data.data
    localStorage.setItem('accessToken', tokenData.accessToken)
    localStorage.setItem('refreshToken', tokenData.refreshToken)
    localStorage.setItem('user', JSON.stringify(tokenData.user))
    setUser(tokenData.user)
  }, [])

  const logout = useCallback(() => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => {})
    }
    localStorage.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
