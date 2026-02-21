import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '@/services/api'
import { useAuth } from '@/store/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { StatusBadge } from '@/components/orders/StatusBadge'
import { ChannelBadge } from '@/components/orders/ChannelBadge'
import { formatCurrency, formatDate } from '@/lib/utils'
import { ArrowLeft, Clock } from 'lucide-react'

const STATUS_TRANSITIONS: Record<string, string[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED', 'RETURNED'],
  DELIVERED: ['RETURNED'],
  CANCELLED: [],
  RETURNED: [],
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [newStatus, setNewStatus] = useState('')
  const [notes, setNotes] = useState('')

  const { data: order, isLoading } = useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.getById(id!).then((r) => r.data.data),
    enabled: !!id,
  })

  const statusMutation = useMutation({
    mutationFn: ({ status, notes }: { status: string; notes?: string }) =>
      ordersApi.updateStatus(id!, status, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order', id] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setNewStatus('')
      setNotes('')
    },
  })

  const canUpdateStatus = user?.role === 'SUPER_ADMIN' || user?.role === 'ADMIN'
  const availableTransitions = order ? STATUS_TRANSITIONS[order.status] || [] : []

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="h-8 w-48 animate-pulse rounded bg-muted" />
        <div className="h-96 animate-pulse rounded bg-muted" />
      </div>
    )
  }

  if (!order) {
    return <div className="text-center text-muted-foreground">Order not found</div>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/orders')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">{order.externalOrderId}</h1>
          <div className="flex items-center gap-2 mt-1">
            <ChannelBadge channel={order.channel} />
            <StatusBadge status={order.status} />
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Customer Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-2 text-sm">
              <span className="text-muted-foreground">Name</span>
              <span className="font-medium">{order.customerName}</span>
              <span className="text-muted-foreground">Email</span>
              <span>{order.customerEmail}</span>
              <span className="text-muted-foreground">Phone</span>
              <span>{order.customerPhone}</span>
              <span className="text-muted-foreground">Address</span>
              <span>{order.shippingAddress}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Order Summary</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-2 text-sm">
              <span className="text-muted-foreground">Total Amount</span>
              <span className="text-lg font-bold">{formatCurrency(order.totalAmount)}</span>
              <span className="text-muted-foreground">Currency</span>
              <span>{order.currency}</span>
              <span className="text-muted-foreground">Order Date</span>
              <span>{formatDate(order.orderedAt)}</span>
              <span className="text-muted-foreground">Last Updated</span>
              <span>{formatDate(order.updatedAt)}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {canUpdateStatus && availableTransitions.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Update Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-end gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">New Status</label>
                <Select value={newStatus} onValueChange={setNewStatus}>
                  <SelectTrigger className="w-[200px]">
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableTransitions.map((s) => (
                      <SelectItem key={s} value={s}>{s}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex-1 space-y-2">
                <label className="text-sm font-medium">Notes (optional)</label>
                <Input
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Add a note about this status change"
                />
              </div>
              <Button
                onClick={() => statusMutation.mutate({ status: newStatus, notes: notes || undefined })}
                disabled={!newStatus || statusMutation.isPending}
              >
                {statusMutation.isPending ? 'Updating...' : 'Update Status'}
              </Button>
            </div>
            {statusMutation.isError && (
              <p className="mt-2 text-sm text-destructive">
                {(statusMutation.error as any)?.response?.data?.message || 'Failed to update status'}
              </p>
            )}
          </CardContent>
        </Card>
      )}

      {order.items && order.items.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Order Items</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product</TableHead>
                  <TableHead>SKU</TableHead>
                  <TableHead className="text-right">Qty</TableHead>
                  <TableHead className="text-right">Unit Price</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {order.items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.productName}</TableCell>
                    <TableCell><Badge variant="outline" className="font-mono text-xs">{item.sku}</Badge></TableCell>
                    <TableCell className="text-right">{item.quantity}</TableCell>
                    <TableCell className="text-right">{formatCurrency(item.unitPrice)}</TableCell>
                    <TableCell className="text-right font-medium">{formatCurrency(item.totalPrice)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {order.statusHistory && order.statusHistory.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Status History</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {order.statusHistory.map((entry) => (
                <div key={entry.id} className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <div className="w-px flex-1 bg-border" />
                  </div>
                  <div className="flex-1 pb-4">
                    <div className="flex items-center gap-2">
                      {entry.oldStatus && <StatusBadge status={entry.oldStatus} />}
                      {entry.oldStatus && <span className="text-muted-foreground">→</span>}
                      <StatusBadge status={entry.newStatus} />
                    </div>
                    {entry.notes && <p className="mt-1 text-sm text-muted-foreground">{entry.notes}</p>}
                    <p className="mt-1 text-xs text-muted-foreground">{formatDate(entry.changedAt)}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
