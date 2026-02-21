import { useQuery } from '@tanstack/react-query'
import { ordersApi } from '@/services/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { formatCurrency } from '@/lib/utils'
import { ShoppingCart, DollarSign, TrendingUp, Package } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'

const STATUS_COLORS: Record<string, string> = {
  PENDING: '#f59e0b',
  CONFIRMED: '#3b82f6',
  PROCESSING: '#6366f1',
  SHIPPED: '#06b6d4',
  DELIVERED: '#22c55e',
  CANCELLED: '#ef4444',
  RETURNED: '#a855f7',
}

const CHANNEL_COLORS: Record<string, string> = {
  AMAZON: '#f97316',
  FLIPKART: '#3b82f6',
  WEBSITE: '#10b981',
}

export function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: () => ordersApi.getStats().then((r) => r.data.data),
    refetchInterval: 30000,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardContent className="p-6"><div className="h-16 animate-pulse rounded bg-muted" /></CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  const stats = data!
  const channelChartData = stats.channelStats.map((c) => ({
    name: c.channel,
    orders: c.orderCount,
    revenue: c.revenue,
  }))

  const statusChartData = Object.entries(stats.statusBreakdown)
    .filter(([, count]) => count > 0)
    .map(([status, count]) => ({
      name: status,
      value: count,
    }))

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Dashboard</h1>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Total Orders</CardTitle>
            <ShoppingCart className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalOrders}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Total Revenue</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCurrency(stats.totalRevenue)}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Today's Orders</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.todayOrders}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">Channels</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.channelStats.length}</div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Revenue by Channel</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={channelChartData}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="name" className="text-xs" />
                <YAxis className="text-xs" />
                <Tooltip formatter={(value: number) => formatCurrency(value)} />
                <Bar dataKey="revenue" radius={[4, 4, 0, 0]}>
                  {channelChartData.map((entry) => (
                    <Cell key={entry.name} fill={CHANNEL_COLORS[entry.name] || '#8884d8'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Order Status Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={statusChartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={2}
                  dataKey="value"
                  nameKey="name"
                >
                  {statusChartData.map((entry) => (
                    <Cell key={entry.name} fill={STATUS_COLORS[entry.name] || '#8884d8'} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
