import axios from 'axios'
import type { ApiResponse, TokenResponse, PagedResponse, Order, DashboardStats, FeatureFlag, OrderFilter } from '@/types/api'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const { data } = await axios.post<ApiResponse<TokenResponse>>('/api/auth/refresh', { refreshToken })
          localStorage.setItem('accessToken', data.data.accessToken)
          localStorage.setItem('refreshToken', data.data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`
          return api(originalRequest)
        } catch {
          localStorage.clear()
          window.location.href = '/login'
        }
      } else {
        localStorage.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (email: string, password: string) =>
    api.post<ApiResponse<TokenResponse>>('/auth/login', { email, password }),
  register: (email: string, password: string, fullName: string) =>
    api.post<ApiResponse<TokenResponse>>('/auth/register', { email, password, fullName }),
  refresh: (refreshToken: string) =>
    api.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken }),
  logout: (refreshToken: string) =>
    api.post('/auth/logout', { refreshToken }),
}

export const ordersApi = {
  list: (filter: OrderFilter) => {
    const params = new URLSearchParams()
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== undefined && value !== '' && value !== null) {
        params.append(key, String(value))
      }
    })
    return api.get<ApiResponse<PagedResponse<Order>>>(`/orders?${params}`)
  },
  getById: (id: string) =>
    api.get<ApiResponse<Order>>(`/orders/${id}`),
  updateStatus: (id: string, status: string, notes?: string) =>
    api.put<ApiResponse<Order>>(`/orders/${id}/status`, { status, notes }),
  getStats: () =>
    api.get<ApiResponse<DashboardStats>>('/orders/stats'),
}

export const adminApi = {
  getFeatureFlags: () =>
    api.get<ApiResponse<FeatureFlag[]>>('/admin/feature-flags'),
  toggleFlag: (key: string, enabled: boolean) =>
    api.put<ApiResponse<FeatureFlag>>(`/admin/feature-flags/${key}`, { enabled }),
  syncChannels: () =>
    api.post<ApiResponse<Record<string, number>>>('/admin/sync-channels'),
}

export default api
