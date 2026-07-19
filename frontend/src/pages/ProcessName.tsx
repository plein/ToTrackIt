import { useMemo, useState } from 'react'
import type { ProcessResponse, ProcessTag, TagImpactEntry } from '@/types'
import { fmtRelative, fmtDuration } from '@/lib/format'
import { Icon } from '@/components/Icon'
import { StatusPill } from '@/components/StatusPill'
import { STATUS_TONE } from '@/lib/statusTone'
import { TagChip } from '@/components/TagChip'
import { DeadlineBar } from '@/components/DeadlineBar'
import { ImpactedTags } from '@/components/ImpactedTags'
import { useProcessList, useTagImpact, PAGE_SIZE } from '@/hooks/useProcesses'

const now = () => Math.floor(Date.now() / 1000)

const PERIODS = [
  { hours: 1, label: '1h' },
  { hours: 24, label: '24h' },
  { hours: 168, label: '7d' },
  { hours: 720, label: '30d' },
]

interface StatProps {
  label: string
  value: string | number
  sub?: string
  tone?: string
}

function Stat({ label, value, sub, tone = 'neutral' }: StatProps) {
  return (
    <div className={`tti-stat tti-stat--${tone}`}>
      <div className="tti-stat__label">{label}</div>
      <div className="tti-stat__value">{value}</div>
      {sub && <div className="tti-stat__sub">{sub}</div>}
    </div>
  )
}

interface ProcessNameProps {
  name: string
  onBack: () => void
  onOpenProcess: (p: ProcessResponse) => void
  onComplete: (p: ProcessResponse, status: 'COMPLETED' | 'FAILED') => void
}

// Per-name "incident page": runs of one process name (server-side filtered and
// paginated), with a period picker driving the impacted-tags breakdown and
// completion latency stats (avg/p50/p90/p99) overall and per tag.
export function ProcessName({ name, onBack, onOpenProcess, onComplete }: ProcessNameProps) {
  const n = now()
  const [windowHours, setWindowHours] = useState(24)
  const [activeTags, setActiveTags] = useState<ProcessTag[]>([])
  const [page, setPage] = useState(0)
  const { data: tagImpact } = useTagImpact(name, windowHours)

  // One bounded request: this name's runs, newest first, tag filters applied
  // server-side.
  const { data: runsPage } = useProcessList({
    name,
    tags: activeTags.length ? activeTags.map((t) => `${t.key}:${t.value}`).join(',') : undefined,
    sort_by: 'started_at:desc',
    limit: PAGE_SIZE,
    offset: page * PAGE_SIZE,
  })
  const totalRuns = runsPage?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(totalRuns / PAGE_SIZE))

  // Snap back to the first page when the tag filter changes
  const tagKey = activeTags.map((t) => `${t.key}:${t.value}`).join(',')
  const [prevTagKey, setPrevTagKey] = useState(tagKey)
  if (tagKey !== prevTagKey) {
    setPrevTagKey(tagKey)
    if (page !== 0) setPage(0)
  }

  // Same window semantics as the analytics endpoint: active runs always shown,
  // finished runs only when they finished inside the period.
  const runs = useMemo(() => {
    const since = n - windowHours * 3600
    return (runsPage?.data ?? []).filter(
      (p) => p.status === 'ACTIVE' || (p.completed_at != null && p.completed_at >= since),
    )
  }, [runsPage, windowHours, n])

  const stats = useMemo(() => {
    const overdue = runs.filter((p) => p.deadline_status === 'MISSED').length
    const completed = runs.filter((p) => p.status === 'COMPLETED')
    const onTimeRate = completed.length
      ? Math.round(
          (completed.filter((p) => p.deadline_status === 'COMPLETED_ON_TIME').length / completed.length) * 100,
        )
      : null
    return { overdue, completedCount: completed.length, onTimeRate }
  }, [runs])

  const duration = tagImpact?.duration
  const overallP90 = duration?.p90_seconds ?? null

  // Per-tag latency: slowest p90 first, so the latency factor is impossible to miss
  const latencyRows = useMemo(
    () =>
      (tagImpact?.tags ?? [])
        .filter((t): t is TagImpactEntry & { duration: NonNullable<TagImpactEntry['duration']> } => t.duration != null)
        .sort((a, b) => b.duration.p90_seconds - a.duration.p90_seconds),
    [tagImpact],
  )

  const toggleTag = (t: ProcessTag) => {
    setActiveTags((cur) => {
      const i = cur.findIndex((x) => x.key === t.key && x.value === t.value)
      return i >= 0 ? cur.filter((_, j) => j !== i) : [...cur, t]
    })
  }

  return (
    <div className="tti-dashboard">
      <div className="tti-page-header">
        <div>
          <div className="tti-page-header__eyebrow">
            <button
              type="button"
              onClick={onBack}
              style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', color: 'inherit', font: 'inherit' }}
            >
              ← Processes
            </button>
          </div>
          <h1 className="tti-page-header__title">{name}</h1>
          <div className="tti-page-header__sub">
            All runs, impact, and completion latency for this process.
          </div>
        </div>
        <div className="tti-page-header__actions">
          {PERIODS.map((p) => (
            <button
              key={p.hours}
              type="button"
              className={`tti-chip${windowHours === p.hours ? ' tti-chip--active' : ''}`}
              onClick={() => setWindowHours(p.hours)}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      <div className="tti-stats">
        <Stat
          label={`Runs (${windowHours}h)`}
          value={tagImpact?.total_processes ?? runs.length}
          sub={`${totalRuns} all time`}
          tone="blue"
        />
        <Stat
          label="Overdue now"
          value={stats.overdue}
          sub={stats.overdue ? 'Needs attention' : 'Nothing past due'}
          tone={stats.overdue ? 'red' : 'neutral'}
        />
        <Stat
          label="On-time rate"
          value={stats.onTimeRate != null ? `${stats.onTimeRate}%` : '—'}
          sub="of completed runs"
          tone={stats.onTimeRate != null && stats.onTimeRate < 100 ? 'amber' : 'green'}
        />
        <Stat
          label="Avg duration"
          value={duration ? fmtDuration(Math.round(duration.avg_seconds)) : '—'}
          sub={duration ? `${duration.count} completions` : 'no completions in period'}
        />
        <Stat
          label="P90 / P99"
          value={duration ? fmtDuration(Math.round(duration.p90_seconds)) : '—'}
          sub={duration ? `p99 ${fmtDuration(Math.round(duration.p99_seconds))}` : ''}
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

      {latencyRows.length > 0 && (
        <div
          style={{
            border: '1px solid var(--border)',
            borderRadius: '10px',
            background: 'var(--surface-1)',
            padding: '14px 16px',
            marginBottom: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '10px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-1)' }}>
              <Icon name="activity" size={13} /> Latency by tag
            </span>
            <span style={{ fontSize: '11px', color: 'var(--text-3)' }}>
              completion duration of finished runs · last {windowHours}h · slowest p90 first
            </span>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
            <thead>
              <tr style={{ color: 'var(--text-3)', textAlign: 'left' }}>
                <th style={{ fontWeight: 500, padding: '4px 8px 6px 0' }}>Tag</th>
                <th style={{ fontWeight: 500, padding: '4px 8px 6px 0', textAlign: 'right' }}>Completions</th>
                <th style={{ fontWeight: 500, padding: '4px 8px 6px 0', textAlign: 'right' }}>Problems</th>
                <th style={{ fontWeight: 500, padding: '4px 8px 6px 0', textAlign: 'right' }}>Avg</th>
                <th style={{ fontWeight: 500, padding: '4px 8px 6px 0', textAlign: 'right' }}>P50</th>
                <th style={{ fontWeight: 500, padding: '4px 0 6px 0', textAlign: 'right' }}>P90</th>
              </tr>
            </thead>
            <tbody>
              {latencyRows.map((t) => {
                const slow = overallP90 != null && t.duration.p90_seconds > overallP90
                return (
                  <tr key={`${t.key}:${t.value}`} style={{ borderTop: '1px solid var(--border)' }}>
                    <td style={{ padding: '6px 8px 6px 0' }}>
                      <TagChip
                        tagKey={t.key}
                        value={t.value}
                        onClick={() => toggleTag({ key: t.key, value: t.value })}
                        active={activeTags.some((at) => at.key === t.key && at.value === t.value)}
                      />
                    </td>
                    <td style={{ padding: '6px 8px 6px 0', textAlign: 'right', color: 'var(--text-2)' }}>{t.duration.count}</td>
                    <td style={{ padding: '6px 8px 6px 0', textAlign: 'right', color: t.problems ? 'var(--red, #d64545)' : 'var(--text-3)' }}>
                      {t.problems || '—'}
                    </td>
                    <td style={{ padding: '6px 8px 6px 0', textAlign: 'right', color: 'var(--text-2)' }}>
                      {fmtDuration(Math.round(t.duration.avg_seconds))}
                    </td>
                    <td style={{ padding: '6px 8px 6px 0', textAlign: 'right', color: 'var(--text-2)' }}>
                      {fmtDuration(Math.round(t.duration.p50_seconds))}
                    </td>
                    <td style={{ padding: '6px 0 6px 0', textAlign: 'right', fontWeight: slow ? 600 : 400, color: slow ? 'var(--amber, #d69e2e)' : 'var(--text-2)' }}>
                      {fmtDuration(Math.round(t.duration.p90_seconds))}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <div className="tti-table tti-table--comfortable">
        <div className="tti-table__head">
          <div className="tti-th tti-col-status">Status</div>
          <div className="tti-th tti-col-name">Run</div>
          <div className="tti-th tti-col-tags">Tags</div>
          <div className="tti-th tti-col-bar">Deadline</div>
          <div className="tti-th tti-col-started">Started</div>
          <div className="tti-th tti-col-duration">Duration</div>
          <div className="tti-th tti-col-actions" />
        </div>
        <div className="tti-table__body">
          {runs.map((p) => (
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
          {runs.length === 0 && (
            <div className="tti-empty">
              <div className="tti-empty__title">No runs in this period</div>
              <div className="tti-empty__sub">Try a longer period, or clear the tag filter.</div>
            </div>
          )}
        </div>
      </div>

      {totalRuns > PAGE_SIZE && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '10px', marginTop: '12px' }}>
          <span style={{ fontSize: '12px', color: 'var(--text-3)' }}>
            {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, totalRuns)} of {totalRuns} runs
          </span>
          <button
            type="button"
            className="tti-btn tti-btn--ghost tti-btn--sm"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            ← Prev
          </button>
          <button
            type="button"
            className="tti-btn tti-btn--ghost tti-btn--sm"
            onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
            disabled={page >= pageCount - 1}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  )
}
