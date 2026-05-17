import { useQuery } from '@tanstack/react-query'
import { listProcesses } from '@/api/processes'
import type { ProcessResponse } from '@/types'
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { Icon } from '@/components/Icon'
import { StatusPill } from '@/components/StatusPill'
import { fmtRelative } from '@/lib/format'

const STATUS_COLORS = { ACTIVE: 'oklch(0.55 0.15 250)', COMPLETED: 'oklch(0.55 0.15 145)', FAILED: 'oklch(0.55 0.2 25)' }
const DEADLINE_COLORS: Record<string, string> = {
  ON_TRACK: 'oklch(0.55 0.15 145)',
  MISSED: 'oklch(0.55 0.2 25)',
  COMPLETED_ON_TIME: 'oklch(0.55 0.15 145)',
  COMPLETED_LATE: 'oklch(0.6 0.15 70)',
}

async function fetchAllForMetrics() {
  let offset = 0
  const all: ProcessResponse[] = []
  while (true) {
    const page = await listProcesses({ limit: 100, offset })
    all.push(...page.data)
    if (!page.has_more) break
    offset += 100
  }
  return all
}

function useAllProcesses() {
  return useQuery({
    queryKey: ['processes-all-metrics'],
    queryFn: fetchAllForMetrics,
    staleTime: 30_000,
  })
}

function aggregate(processes: ProcessResponse[]) {
  const statusCounts = { ACTIVE: 0, COMPLETED: 0, FAILED: 0 }
  const deadlineCounts: Record<string, number> = {
    ON_TRACK: 0, MISSED: 0, COMPLETED_ON_TIME: 0, COMPLETED_LATE: 0,
  }
  let missedCount = 0
  let completedLast24h = 0
  let completedOnTimeCount = 0
  const n = Date.now() / 1000

  for (const p of processes) {
    statusCounts[p.status]++
    if (p.deadline_status) deadlineCounts[p.deadline_status]++
    if (p.deadline_status === 'MISSED') missedCount++
    if (p.completed_at && n - p.completed_at < 86400) {
      completedLast24h++
      if (p.deadline_status === 'COMPLETED_ON_TIME') completedOnTimeCount++
    }
  }

  const completionRate = completedLast24h > 0
    ? Math.round((completedOnTimeCount / completedLast24h) * 100)
    : null

  return { statusCounts, deadlineCounts, missedCount, completedLast24h, completionRate }
}

export function Metrics() {
  const { data, isFetching, refetch } = useAllProcesses()
  const processes = data ?? []
  const { statusCounts, deadlineCounts, missedCount, completedLast24h, completionRate } = aggregate(processes)
  const n = Math.floor(Date.now() / 1000)

  const statusData = Object.entries(statusCounts)
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name, value }))

  const deadlineData = Object.entries(deadlineCounts)
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name: name.replace(/_/g, ' '), rawName: name, value }))

  return (
    <div className="tti-dashboard">
      <div className="tti-page-header">
        <div>
          <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
          <h1 className="tti-page-header__title">Metrics</h1>
          <div className="tti-page-header__sub">Status and deadline health across all tracked processes.</div>
        </div>
        <div className="tti-page-header__actions">
          <button
            type="button"
            className={`tti-btn tti-btn--ghost tti-btn--md${isFetching ? ' tti-btn--loading' : ''}`}
            onClick={() => refetch()}
            title="Refresh"
          >
            <Icon name="refresh" size={16} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      <div className="tti-stats">
        <div className="tti-stat tti-stat--blue">
          <div className="tti-stat__label">Active Processes</div>
          <div className="tti-stat__value">{statusCounts.ACTIVE}</div>
        </div>
        <div className={`tti-stat tti-stat--${missedCount ? 'red' : 'neutral'}`}>
          <div className="tti-stat__label">Missed Deadlines</div>
          <div className="tti-stat__value">{missedCount}</div>
        </div>
        <div className="tti-stat tti-stat--green">
          <div className="tti-stat__label">Completed (24h)</div>
          <div className="tti-stat__value">{completedLast24h}</div>
        </div>
        <div className="tti-stat tti-stat--green">
          <div className="tti-stat__label">On-time Rate (24h)</div>
          <div className="tti-stat__value">{completionRate !== null ? `${completionRate}%` : '—'}</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-2)', marginBottom: '16px' }}>Status Distribution</div>
          {statusData.length === 0 ? (
            <div className="tti-empty" style={{ padding: '32px 0' }}>
              <div className="tti-empty__title">No data yet</div>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie
                  data={statusData}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={90}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {statusData.map((entry) => (
                    <Cell key={entry.name} fill={STATUS_COLORS[entry.name as keyof typeof STATUS_COLORS]} />
                  ))}
                </Pie>
                <Tooltip formatter={(v) => [v, 'Processes']} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-2)', marginBottom: '16px' }}>Deadline Status</div>
          {deadlineData.length === 0 ? (
            <div className="tti-empty" style={{ padding: '32px 0' }}>
              <div className="tti-empty__title">No data yet</div>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={deadlineData} barCategoryGap="35%">
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="value" name="Processes" radius={[4, 4, 0, 0]}>
                  {deadlineData.map((entry) => (
                    <Cell key={entry.rawName} fill={DEADLINE_COLORS[entry.rawName]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '12px', overflow: 'hidden' }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', fontSize: '13px', fontWeight: 600, color: 'var(--text-2)' }}>Recent Activity</div>
        {processes.length === 0 ? (
          <div className="tti-empty" style={{ padding: '32px 0' }}>
            <div className="tti-empty__title">No processes yet</div>
          </div>
        ) : (
          <div className="tti-kv">
            {processes.slice(0, 10).map((p) => (
              <div key={`${p.name}-${p.id}`} className="tti-kv__row" style={{ alignItems: 'center' }}>
                <div className="tti-kv__k" style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                  <span>{p.name}</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', color: 'var(--text-3)' }}>{p.id}</span>
                </div>
                <div className="tti-kv__v" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <StatusPill value={p.status} />
                  <span style={{ color: 'var(--text-3)', fontSize: '12px' }}>{fmtRelative(p.started_at, n)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
