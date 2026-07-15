import { useState, useMemo, useEffect, useRef } from 'react'
import type { ProcessResponse, ProcessTag } from '@/types'
import { fmtRelative, fmtDuration } from '@/lib/format'
import { Icon } from '@/components/Icon'
import { Button } from '@/components/Button'
import { StatusPill } from '@/components/StatusPill'
import { STATUS_TONE } from '@/lib/statusTone'
import { TagChip } from '@/components/TagChip'
import { DeadlineBar } from '@/components/DeadlineBar'
import { ImpactedTags } from '@/components/ImpactedTags'
import { useTagImpact } from '@/hooks/useProcesses'

const now = () => Math.floor(Date.now() / 1000)

// ---- Stat card ----
interface StatCardProps {
  label: string
  value: string | number
  sub?: string
  tone?: string
  trend?: number[]
}

function StatCard({ label, value, sub, tone = 'neutral', trend }: StatCardProps) {
  return (
    <div className={`tti-stat tti-stat--${tone}`}>
      <div className="tti-stat__label">{label}</div>
      <div className="tti-stat__value">{value}</div>
      {sub && <div className="tti-stat__sub">{sub}</div>}
      {trend && (
        <svg className="tti-stat__spark" viewBox="0 0 100 28" preserveAspectRatio="none" aria-hidden="true">
          <polyline
            points={trend.map((v, i) => `${(i / (trend.length - 1)) * 100},${28 - v * 26}`).join(' ')}
            fill="none"
            stroke="currentColor"
            strokeWidth="1.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      )}
    </div>
  )
}

// ---- Filter chip dropdown ----
interface FilterOption {
  value: string
  label: string
  tone?: string
  count?: number
}

interface FilterChipProps {
  label: string
  value: string
  options: FilterOption[]
  onChange: (v: string) => void
}

function FilterChip({ label, value, options, onChange }: FilterChipProps) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [])
  const active = value && value !== 'all'
  const current = options.find((o) => o.value === value)
  return (
    <div className="tti-filter" ref={ref}>
      <button
        type="button"
        className={`tti-filter__btn${active ? ' tti-filter__btn--active' : ''}`}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="tti-filter__label">{label}</span>
        {active && <span className="tti-filter__divider" />}
        {active && <span className="tti-filter__value">{current?.label}</span>}
        <Icon name="chevron_down" size={14} />
      </button>
      {open && (
        <div className="tti-filter__menu" role="menu">
          {options.map((o) => (
            <button
              key={o.value}
              type="button"
              className={`tti-filter__item${o.value === value ? ' tti-filter__item--active' : ''}`}
              onClick={() => { onChange(o.value); setOpen(false) }}
            >
              {o.tone && (
                <span className={`tti-dot tti-dot--${o.tone}`} aria-hidden="true" />
              )}
              <span>{o.label}</span>
              {o.count != null && <span className="tti-filter__count">{o.count}</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

// ---- Main Dashboard ----
interface DashboardProps {
  processes: ProcessResponse[]
  onOpenProcess: (p: ProcessResponse) => void
  onOpenCreate: () => void
  onComplete: (p: ProcessResponse, status: 'COMPLETED' | 'FAILED') => void
  onOpenName?: (name: string) => void
  density?: string
  initialNameFilter?: string | null
  initialTagFilter?: { key: string; value: string } | null
  navKey?: string
  onRefresh?: () => void
  isFetching?: boolean
}

export function Dashboard({
  processes,
  onOpenProcess,
  onOpenCreate,
  onComplete,
  onOpenName,
  density = 'comfortable',
  initialNameFilter,
  initialTagFilter,
  navKey,
  onRefresh,
  isFetching = false,
}: DashboardProps) {
  const n = now()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [deadlineFilter, setDeadlineFilter] = useState('all')
  const [nameFilter, setNameFilter] = useState(initialNameFilter || 'all')
  const [activeTags, setActiveTags] = useState<ProcessTag[]>(initialTagFilter ? [initialTagFilter] : [])
  const [sortField, setSortField] = useState<keyof ProcessResponse>('started_at')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')
  const { data: tagImpact } = useTagImpact()

  // Reset filters when navigation changes; render-phase state adjustment
  // instead of an effect (see React docs on resetting state when a prop changes)
  const resetKey = `${navKey}|${initialNameFilter ?? ''}|${initialTagFilter ? `${initialTagFilter.key}=${initialTagFilter.value}` : ''}`
  const [prevResetKey, setPrevResetKey] = useState(resetKey)
  if (resetKey !== prevResetKey) {
    setPrevResetKey(resetKey)
    setNameFilter(initialNameFilter || 'all')
    setStatusFilter('all')
    setDeadlineFilter('all')
    setActiveTags(initialTagFilter ? [initialTagFilter] : [])
    setSearch('')
  }

  const stats = useMemo(() => {
    const active = processes.filter((p) => p.status === 'ACTIVE').length
    const missed = processes.filter((p) => p.deadline_status === 'MISSED').length
    const failed = processes.filter((p) => p.status === 'FAILED').length
    const completedToday = processes.filter(
      (p) => p.status === 'COMPLETED' && p.completed_at != null && n - p.completed_at < 86400
    ).length
    const done = processes.filter((p) => p.status === 'COMPLETED')
    const onTimeRate = done.length
      ? Math.round((done.filter((p) => p.deadline_status === 'COMPLETED_ON_TIME').length / done.length) * 100)
      : 100
    return { total: processes.length, active, missed, failed, completedToday, onTimeRate }
  }, [processes, n])

  const names = useMemo(() => {
    const m = new Map<string, number>()
    processes.forEach((p) => m.set(p.name, (m.get(p.name) || 0) + 1))
    return [...m.entries()].sort((a, b) => b[1] - a[1])
  }, [processes])

  const filtered = useMemo(() => {
    let list = processes.slice()
    const q = search.trim().toLowerCase()
    if (q) {
      list = list.filter(
        (p) =>
          p.id.toLowerCase().includes(q) ||
          p.name.toLowerCase().includes(q) ||
          p.tags.some((t) => `${t.key}:${t.value}`.toLowerCase().includes(q))
      )
    }
    if (statusFilter !== 'all') list = list.filter((p) => p.status === statusFilter)
    if (deadlineFilter !== 'all') list = list.filter((p) => p.deadline_status === deadlineFilter)
    if (nameFilter !== 'all') list = list.filter((p) => p.name === nameFilter)
    if (activeTags.length) {
      list = list.filter((p) =>
        activeTags.every((t) => p.tags.some((pt) => pt.key === t.key && pt.value === t.value))
      )
    }
    list.sort((a, b) => {
      const av = (a[sortField] as number) ?? 0
      const bv = (b[sortField] as number) ?? 0
      return sortDir === 'desc' ? bv - av : av - bv
    })
    return list
  }, [processes, search, statusFilter, deadlineFilter, nameFilter, activeTags, sortField, sortDir])

  const toggleTag = (t: ProcessTag) => {
    setActiveTags((cur) => {
      const i = cur.findIndex((x) => x.key === t.key && x.value === t.value)
      return i >= 0 ? cur.filter((_, j) => j !== i) : [...cur, t]
    })
  }

  const headerSort = (field: keyof ProcessResponse) => {
    if (sortField === field) setSortDir((d) => (d === 'desc' ? 'asc' : 'desc'))
    else { setSortField(field); setSortDir('desc') }
  }

  const sortInd = (field: keyof ProcessResponse) =>
    sortField === field ? (sortDir === 'desc' ? '↓' : '↑') : ''

  const clearFilters = () => {
    setSearch(''); setStatusFilter('all'); setDeadlineFilter('all')
    setNameFilter('all'); setActiveTags([])
  }
  const hasFilters = search || statusFilter !== 'all' || deadlineFilter !== 'all' || nameFilter !== 'all' || activeTags.length > 0

  // No processes at all in this workspace (not a filter issue)
  if (processes.length === 0) {
    return (
      <div className="tti-dashboard">
        <div className="tti-page-header">
          <div>
            <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
            <h1 className="tti-page-header__title">Processes</h1>
          </div>
          <div className="tti-page-header__actions">
            <Button variant="primary" icon="plus" onClick={onOpenCreate}>New process</Button>
          </div>
        </div>
        <div className="tti-empty" style={{ marginTop: '40px' }}>
          <Icon name="activity" size={32} />
          <div className="tti-empty__title">No processes tracked yet</div>
          <div className="tti-empty__sub">
            Register your first process from the UI or via the API:
          </div>
          <pre style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: '8px', padding: '12px 16px', color: 'var(--text-2)', textAlign: 'left', marginTop: '4px' }}>
{`curl -X POST http://localhost:8080/processes/my-job \\
  -H "Content-Type: application/json" \\
  -d '{"id":"run-001","deadline":1893456000}'`}
          </pre>
          <Button variant="primary" icon="plus" onClick={onOpenCreate} style={{ marginTop: '4px' }}>Register from UI</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="tti-dashboard">
      <div className="tti-page-header">
        <div>
          <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
          <h1 className="tti-page-header__title">Processes</h1>
          <div className="tti-page-header__sub">
            Live view of every async job, batch, and worker — with deadlines they're chasing.
          </div>
        </div>
        <div className="tti-page-header__actions">
          <Button variant="ghost" icon="refresh" onClick={onRefresh} disabled={isFetching} style={isFetching ? { opacity: 0.6 } : undefined}>
            {isFetching ? 'Refreshing…' : 'Refresh'}
          </Button>
          <Button variant="primary" icon="plus" onClick={onOpenCreate}>New process</Button>
        </div>
      </div>

      <div className="tti-stats">
        <StatCard
          label="Active right now"
          value={stats.active}
          sub={`${stats.total} total tracked`}
          tone="blue"
          trend={[0.4, 0.45, 0.55, 0.5, 0.6, 0.65, 0.7, 0.66, 0.72, 0.74, 0.78, 0.82]}
        />
        <StatCard
          label="Missed deadlines"
          value={stats.missed}
          sub={stats.missed ? 'Needs attention' : 'Nothing past due'}
          tone={stats.missed ? 'red' : 'neutral'}
          trend={[0.2, 0.25, 0.18, 0.22, 0.4, 0.35, 0.32, 0.55, 0.5, 0.58, 0.62, 0.6]}
        />
        <StatCard
          label="Failed (24h)"
          value={stats.failed}
          sub={`${stats.failed} processes`}
          tone={stats.failed ? 'amber' : 'neutral'}
          trend={[0.1, 0.12, 0.08, 0.1, 0.14, 0.12, 0.18, 0.16, 0.22, 0.18, 0.24, 0.28]}
        />
        <StatCard
          label="On-time rate"
          value={`${stats.onTimeRate}%`}
          sub={`${stats.completedToday} completed today`}
          tone="green"
          trend={[0.7, 0.72, 0.74, 0.7, 0.76, 0.78, 0.8, 0.82, 0.78, 0.84, 0.86, 0.88]}
        />
      </div>

      {tagImpact && (
        <ImpactedTags
          entries={tagImpact.tags}
          windowHours={tagImpact.window_hours}
          activeTags={activeTags}
          onToggleTag={toggleTag}
        />
      )}

      <div className="tti-filterbar">
        <div className="tti-search">
          <Icon name="search" size={16} />
          <input
            type="text"
            placeholder="Search by id, name, or tag…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <button type="button" className="tti-search__clear" onClick={() => setSearch('')}>
              <Icon name="x" size={14} />
            </button>
          )}
        </div>
        <div className="tti-filterbar__divider" />
        <FilterChip
          label="Status"
          value={statusFilter}
          onChange={setStatusFilter}
          options={[
            { value: 'all', label: 'All' },
            { value: 'ACTIVE', label: 'Active', tone: 'blue', count: processes.filter((p) => p.status === 'ACTIVE').length },
            { value: 'COMPLETED', label: 'Completed', tone: 'green', count: processes.filter((p) => p.status === 'COMPLETED').length },
            { value: 'FAILED', label: 'Failed', tone: 'red', count: processes.filter((p) => p.status === 'FAILED').length },
          ]}
        />
        <FilterChip
          label="Deadline"
          value={deadlineFilter}
          onChange={setDeadlineFilter}
          options={[
            { value: 'all', label: 'All' },
            { value: 'ON_TRACK', label: 'On track', tone: 'green' },
            { value: 'MISSED', label: 'Missed', tone: 'red' },
            { value: 'COMPLETED_ON_TIME', label: 'On time', tone: 'green' },
            { value: 'COMPLETED_LATE', label: 'Late', tone: 'amber' },
          ]}
        />
        <FilterChip
          label="Name"
          value={nameFilter}
          onChange={setNameFilter}
          options={[
            { value: 'all', label: 'All names' },
            ...names.map(([nm, c]) => ({ value: nm, label: nm, count: c })),
          ]}
        />
        {activeTags.map((t) => (
          <TagChip
            key={`${t.key}:${t.value}`}
            tagKey={t.key}
            value={t.value}
            active
            onClick={() => toggleTag(t)}
          />
        ))}
        {hasFilters && (
          <button type="button" className="tti-filterbar__clear" onClick={clearFilters}>
            Clear all
          </button>
        )}
        <div className="tti-filterbar__spacer" />
        <div className="tti-filterbar__count">
          <b>{filtered.length}</b> of {processes.length}
        </div>
      </div>

      <div className={`tti-table tti-table--${density}`}>
        <div className="tti-table__head">
          <div className="tti-th tti-col-status">
            <button type="button" onClick={() => headerSort('status')}>Status {sortInd('status')}</button>
          </div>
          <div className="tti-th tti-col-name">
            <button type="button" onClick={() => headerSort('name')}>Process</button>
          </div>
          <div className="tti-th tti-col-tags">Tags</div>
          <div className="tti-th tti-col-bar">Deadline</div>
          <div className="tti-th tti-col-started">
            <button type="button" onClick={() => headerSort('started_at')}>Started {sortInd('started_at')}</button>
          </div>
          <div className="tti-th tti-col-duration">
            <button type="button" onClick={() => headerSort('duration')}>Duration {sortInd('duration')}</button>
          </div>
          <div className="tti-th tti-col-actions" />
        </div>
        <div className="tti-table__body">
          {filtered.map((p) => (
            <div
              key={`${p.name}/${p.id}`}
              className={`tti-row tti-row--${STATUS_TONE[p.deadline_status ?? ''] || 'neutral'}`}
              onClick={() => onOpenProcess(p)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => { if (e.key === 'Enter') onOpenProcess(p) }}
            >
              <div className="tti-td tti-col-status">
                <StatusPill value={p.status} />
              </div>
              <div className="tti-td tti-col-name">
                <button
                  type="button"
                  className="tti-row__name tti-row__name--link"
                  onClick={(e) => {
                    e.stopPropagation()
                    if (onOpenName) onOpenName(p.name)
                    else setNameFilter(p.name)
                  }}
                  title={`Open ${p.name}: all runs, impact, and latency`}
                >
                  {p.name}
                </button>
                <div className="tti-row__id">{p.id}</div>
              </div>
              <div className="tti-td tti-col-tags">
                <div className="tti-row__tags">
                  {p.tags.slice(0, 3).map((t) => (
                    <TagChip
                      key={`${t.key}:${t.value}`}
                      tagKey={t.key}
                      value={t.value}
                      onClick={(e) => { e.stopPropagation(); toggleTag(t) }}
                      active={activeTags.some((at) => at.key === t.key && at.value === t.value)}
                    />
                  ))}
                  {p.tags.length > 3 && <span className="tti-row__tags-more">+{p.tags.length - 3}</span>}
                </div>
              </div>
              <div className="tti-td tti-col-bar">
                <DeadlineBar proc={p} now={n} />
                <div className="tti-row__deadline-meta">
                  {p.deadline_status && <StatusPill value={p.deadline_status} subtle />}
                </div>
              </div>
              <div className="tti-td tti-col-started">
                <div className="tti-row__started">{fmtRelative(p.started_at, n)}</div>
              </div>
              <div className="tti-td tti-col-duration">
                <div className="tti-row__duration">{fmtDuration(p.duration)}</div>
              </div>
              <div className="tti-td tti-col-actions" onClick={(e) => e.stopPropagation()}>
                {p.status === 'ACTIVE' ? (
                  <div className="tti-row__actions">
                    <button
                      type="button"
                      className="tti-row__action tti-row__action--complete"
                      onClick={() => onComplete(p, 'COMPLETED')}
                      title="Mark complete"
                    >
                      <Icon name="check" size={14} />
                    </button>
                    <button
                      type="button"
                      className="tti-row__action tti-row__action--fail"
                      onClick={() => onComplete(p, 'FAILED')}
                      title="Mark failed"
                    >
                      <Icon name="x" size={14} />
                    </button>
                  </div>
                ) : (
                  <Icon name="chevron_right" size={16} />
                )}
              </div>
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="tti-empty">
              <div className="tti-empty__title">No processes match these filters</div>
              <div className="tti-empty__sub">Try clearing filters or broadening the search.</div>
              <Button variant="ghost" onClick={clearFilters}>Clear filters</Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
