import { useState, useCallback } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ordersApi } from '@/services/api'
import type { OrderFilter } from '@/types/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge } from '@/components/orders/StatusBadge'
import { ChannelBadge } from '@/components/orders/ChannelBadge'
import { formatCurrency, formatDate } from '@/lib/utils'
import { Search, ChevronLeft, ChevronRight, ArrowUpDown } from 'lucide-react'

export function OrdersPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<OrderFilter>({
    page: 0,
    size: 20,
    sortBy: 'orderedAt',
    sortDir: 'desc',
  })
  const [searchInput, setSearchInput] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['orders', filter],
    queryFn: () => ordersApi.list(filter).then((r) => r.data.data),
    placeholderData: (prev) => prev,
  })

  const handleSearch = useCallback(() => {
    setFilter((prev) => ({ ...prev, search: searchInput, page: 0 }))
  }, [searchInput])

  const handleSort = (field: string) => {
    setFilter((prev) => ({
      ...prev,
      sortBy: field,
      sortDir: prev.sortBy === field && prev.sortDir === 'asc' ? 'desc' : 'asc',
    }))
  }

  const SortHeader = ({ field, children }: { field: string; children: React.ReactNode }) => (
    <button
      onClick={() => handleSort(field)}
      className="flex items-center gap-1 font-medium hover:text-foreground"
    >
      {children}
      <ArrowUpDown className="h-3 w-3" />
    </button>
  )

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Orders</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <div className="flex flex-1 items-center gap-2">
              <Input
                placeholder="Search orders, customers, emails..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                className="max-w-sm"
              />
              <Button variant="outline" size="icon" onClick={handleSearch}>
                <Search className="h-4 w-4" />
              </Button>
            </div>
            <Select
              value={filter.channel || 'ALL'}
              onValueChange={(v) => setFilter((prev) => ({ ...prev, channel: v === 'ALL' ? undefined : v, page: 0 }))}
            >
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="Channel" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Channels</SelectItem>
                <SelectItem value="AMAZON">Amazon</SelectItem>
                <SelectItem value="FLIPKART">Flipkart</SelectItem>
                <SelectItem value="WEBSITE">Website</SelectItem>
              </SelectContent>
            </Select>
            <Select
              value={filter.status || 'ALL'}
              onValueChange={(v) => setFilter((prev) => ({ ...prev, status: v === 'ALL' ? undefined : v, page: 0 }))}
            >
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Status</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="CONFIRMED">Confirmed</SelectItem>
                <SelectItem value="PROCESSING">Processing</SelectItem>
                <SelectItem value="SHIPPED">Shipped</SelectItem>
                <SelectItem value="DELIVERED">Delivered</SelectItem>
                <SelectItem value="CANCELLED">Cancelled</SelectItem>
                <SelectItem value="RETURNED">Returned</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead><SortHeader field="externalOrderId">Order ID</SortHeader></TableHead>
                <TableHead><SortHeader field="channel">Channel</SortHeader></TableHead>
                <TableHead><SortHeader field="customerName">Customer</SortHeader></TableHead>
                <TableHead><SortHeader field="status">Status</SortHeader></TableHead>
                <TableHead><SortHeader field="totalAmount">Amount</SortHeader></TableHead>
                <TableHead><SortHeader field="orderedAt">Date</SortHeader></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                [...Array(10)].map((_, i) => (
                  <TableRow key={i}>
                    {[...Array(6)].map((_, j) => (
                      <TableCell key={j}><div className="h-4 animate-pulse rounded bg-muted" /></TableCell>
                    ))}
                  </TableRow>
                ))
              ) : data?.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                    No orders found
                  </TableCell>
                </TableRow>
              ) : (
                data?.content.map((order) => (
                  <TableRow
                    key={order.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/orders/${order.id}`)}
                  >
                    <TableCell className="font-mono text-xs">{order.externalOrderId}</TableCell>
                    <TableCell><ChannelBadge channel={order.channel} /></TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{order.customerName}</p>
                        <p className="text-xs text-muted-foreground">{order.customerEmail}</p>
                      </div>
                    </TableCell>
                    <TableCell><StatusBadge status={order.status} /></TableCell>
                    <TableCell className="font-medium">{formatCurrency(order.totalAmount)}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">{formatDate(order.orderedAt)}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between border-t px-4 py-3">
              <p className="text-sm text-muted-foreground">
                Showing {data.page * data.size + 1}-{Math.min((data.page + 1) * data.size, data.totalElements)} of {data.totalElements}
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.first}
                  onClick={() => setFilter((prev) => ({ ...prev, page: prev.page - 1 }))}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <span className="text-sm">
                  Page {data.page + 1} of {data.totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.last}
                  onClick={() => setFilter((prev) => ({ ...prev, page: prev.page + 1 }))}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
