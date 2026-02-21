import { Badge } from '@/components/ui/badge'

const STATUS_CONFIG: Record<string, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning' | 'info' }> = {
  PENDING: { label: 'Pending', variant: 'warning' },
  CONFIRMED: { label: 'Confirmed', variant: 'info' },
  PROCESSING: { label: 'Processing', variant: 'default' },
  SHIPPED: { label: 'Shipped', variant: 'info' },
  DELIVERED: { label: 'Delivered', variant: 'success' },
  CANCELLED: { label: 'Cancelled', variant: 'destructive' },
  RETURNED: { label: 'Returned', variant: 'secondary' },
}

export function StatusBadge({ status }: { status: string }) {
  const config = STATUS_CONFIG[status] || { label: status, variant: 'outline' as const }
  return <Badge variant={config.variant}>{config.label}</Badge>
}
