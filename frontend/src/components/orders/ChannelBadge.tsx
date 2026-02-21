import { Badge } from '@/components/ui/badge'

const CHANNEL_CONFIG: Record<string, { label: string; className: string }> = {
  AMAZON: { label: 'Amazon', className: 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-100 border-transparent' },
  FLIPKART: { label: 'Flipkart', className: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-100 border-transparent' },
  WEBSITE: { label: 'Website', className: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-100 border-transparent' },
}

export function ChannelBadge({ channel }: { channel: string }) {
  const config = CHANNEL_CONFIG[channel] || { label: channel, className: '' }
  return <Badge className={config.className}>{config.label}</Badge>
}
