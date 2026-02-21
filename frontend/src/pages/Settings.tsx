import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '@/services/api'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { RefreshCw } from 'lucide-react'
import { useState } from 'react'

export function SettingsPage() {
  const queryClient = useQueryClient()
  const [syncResult, setSyncResult] = useState<Record<string, number> | null>(null)

  const { data: flags, isLoading } = useQuery({
    queryKey: ['feature-flags'],
    queryFn: () => adminApi.getFeatureFlags().then((r) => r.data.data),
  })

  const toggleMutation = useMutation({
    mutationFn: ({ key, enabled }: { key: string; enabled: boolean }) =>
      adminApi.toggleFlag(key, enabled),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['feature-flags'] }),
  })

  const syncMutation = useMutation({
    mutationFn: () => adminApi.syncChannels(),
    onSuccess: (res) => setSyncResult(res.data.data),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Settings</h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Channel Sync</CardTitle>
          <CardDescription>Manually trigger order sync from all channels</CardDescription>
        </CardHeader>
        <CardContent>
          <Button
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
          >
            <RefreshCw className={`mr-2 h-4 w-4 ${syncMutation.isPending ? 'animate-spin' : ''}`} />
            {syncMutation.isPending ? 'Syncing...' : 'Sync Now'}
          </Button>
          {syncResult && (
            <div className="mt-4 rounded-md bg-muted p-3 text-sm">
              {Object.entries(syncResult).map(([channel, count]) => (
                <p key={channel}>{channel}: {count} new orders imported</p>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Feature Flags</CardTitle>
          <CardDescription>Enable or disable system features</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-12 animate-pulse rounded bg-muted" />
              ))}
            </div>
          ) : (
            <div className="space-y-4">
              {flags?.map((flag) => (
                <div key={flag.id} className="flex items-center justify-between rounded-lg border p-4">
                  <div>
                    <p className="font-mono text-sm font-medium">{flag.flagKey}</p>
                    <p className="text-sm text-muted-foreground">{flag.description}</p>
                  </div>
                  <button
                    onClick={() => toggleMutation.mutate({ key: flag.flagKey, enabled: !flag.enabled })}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      flag.enabled ? 'bg-primary' : 'bg-muted'
                    }`}
                    disabled={toggleMutation.isPending}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        flag.enabled ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
