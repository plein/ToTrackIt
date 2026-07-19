import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { Icon } from '@/components/Icon'
import { StatusPill } from '@/components/StatusPill'
import { fmtRelative } from '@/lib/format'
import { useSummary, useProcessList } from '@/hooks/useProcesses'

const now = () => Math.floor(Date.now() / 1000)

const STATUS_COLORS = { ACTIVE: 'oklch(0.55 0.15 250)', COMPLETED: 'oklch(0.55 0.15 145)', FAILED: 'oklch(0.55 0.2 25)' }
const DEADLINE_COLORS: Record<string, string> = {
  ON_TRACK: 'oklch(0.55 0.15 145)',
  MISSED: 'oklch(0.55 0.2 25)',
  COMPLETED_ON_TIME: 'oklch(0.55 0.15 145)',
  COMPLETED_LATE: 'oklch(0.6 0.15 70)',
}

// All aggregation happens server-side in /analytics/summary; the recent
// activity list is one bounded page of the newest runs.
export function Metrics() {
  const { data: summary, isFetching, refetch } = useSummary()
  const { data: recentPage } = useProcessList({ sort_by: 'started_at:desc', limit: 10 })
  const recent = recentPage?.data ?? []
  const n = now()

  const statusCounts = {
    ACTIVE: summary?.active ?? 0,
    COMPLETED: summary?.completed ?? 0,
    FAILED: summary?.failed ?? 0,
  }
  const deadlineCounts: Record<string, number> = {
    ON_TRACK: summary?.on_track ?? 0,
    MISSED: summary?.overdue ?? 0,
    COMPLETED_ON_TIME: summary?.completed_on_time ?? 0,
    COMPLETED_LATE: summary?.completed_late ?? 0,
  }
  const missedCount = summary?.overdue ?? 0
  const completedLast24h = summary?.completed_24h ?? 0
  const completionRate = summary && summary.completed_24h > 0
    ? Math.round((summary.completed_on_time_24h / summary.completed_24h) * 100)
    : null

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
        {recent.length === 0 ? (
          <div className="tti-empty" style={{ padding: '32px 0' }}>
            <div className="tti-empty__title">No processes yet</div>
          </div>
        ) : (
          <div className="tti-kv">
            {recent.map((p) => (
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
