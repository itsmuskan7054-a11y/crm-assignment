export interface ApiResponse<T> {
  success: boolean
  message: string | null
  data: T
  errors: Record<string, string> | null
  timestamp: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface UserInfo {
  id: string
  email: string
  fullName: string
  role: string
}

export interface Order {
  id: string
  externalOrderId: string
  channel: string
  status: string
  customerName: string
  customerEmail: string
  customerPhone: string
  shippingAddress?: string
  totalAmount: number
  currency: string
  metadata?: Record<string, unknown>
  orderedAt: string
  updatedAt: string
  items?: OrderItem[]
  statusHistory?: StatusHistory[]
}

export interface OrderItem {
  id: string
  productName: string
  sku: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export interface StatusHistory {
  id: string
  oldStatus: string | null
  newStatus: string
  changedBy: string | null
  notes: string | null
  changedAt: string
}

export interface DashboardStats {
  totalOrders: number
  totalRevenue: number
  todayOrders: number
  channelStats: ChannelStat[]
  statusBreakdown: Record<string, number>
}

export interface ChannelStat {
  channel: string
  orderCount: number
  revenue: number
}

export interface FeatureFlag {
  id: string
  flagKey: string
  enabled: boolean
  description: string
  updatedAt: string
}

export interface OrderFilter {
  channel?: string
  status?: string
  search?: string
  from?: string
  to?: string
  page: number
  size: number
  sortBy: string
  sortDir: string
}
