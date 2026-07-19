import { useState, useEffect, useCallback } from 'react'
import type { ProcessResponse } from '@/types'
import { fmtRelative, fmtDuration, fmtAbsolute } from '@/lib/format'
import { useDeleteProcess, useProcessList } from '@/hooks/useProcesses'
import { Icon } from './Icon'
import { Button } from './Button'
import { StatusPill } from './StatusPill'
import { TagChip } from './TagChip'

const now = () => Math.floor(Date.now() / 1000)

// ---- Timeline ----
function Timeline({ proc }: { proc: ProcessResponse }) {
  const n = now()
  const { started_at, deadline, completed_at, status, deadline_status } = proc
  const points = [started_at, deadline, completed_at, n].filter((x): x is number => x != null)
  const tMin = Math.min(...points)
  const tMax = Math.max(...points)
  const span = Math.max(60, tMax - tMin)
  const pad = span * 0.06
  const t0 = tMin - pad
  const t1 = tMax + pad
  const pos = (t: number) => `${((t - t0) / (t1 - t0)) * 100}%`
  const endRunning = completed_at != null ? completed_at : n
  const tone = status === 'FAILED' ? 'red' : status === 'COMPLETED' ? 'green' : 'blue'
  const isLate = deadline_status === 'MISSED' || deadline_status === 'COMPLETED_LATE'

  return (
    <div className="tti-timeline">
      <div className="tti-timeline__track">
        {deadline && deadline > started_at && (
          <div
            className={`tti-timeline__zone tti-timeline__zone--${isLate ? 'over' : 'ok'}`}
            style={{ left: pos(started_at), width: `calc(${pos(deadline)} - ${pos(started_at)})` }}
          />
        )}
        <div
          className={`tti-timeline__seg tti-timeline__seg--${tone}`}
          style={{ left: pos(started_at), width: `calc(${pos(endRunning)} - ${pos(started_at)})` }}
        />
        <div className="tti-timeline__marker tti-timeline__marker--start" style={{ left: pos(started_at) }}>
          <div className="tti-timeline__pin" />
          <div className="tti-timeline__caption">
            <div className="tti-timeline__caption-l">started</div>
            <div className="tti-timeline__caption-v">{fmtRelative(started_at, n)}</div>
          </div>
        </div>
        {deadline && (
          <div className="tti-timeline__marker tti-timeline__marker--deadline" style={{ left: pos(deadline) }}>
            <div className="tti-timeline__pin tti-timeline__pin--deadline" />
            <div className="tti-timeline__caption tti-timeline__caption--top">
              <div className="tti-timeline__caption-l">deadline</div>
              <div className="tti-timeline__caption-v">{fmtRelative(deadline, n)}</div>
            </div>
          </div>
        )}
        {completed_at != null && (
          <div className="tti-timeline__marker" style={{ left: pos(completed_at) }}>
            <div className={`tti-timeline__pin tti-timeline__pin--${status === 'FAILED' ? 'fail' : 'done'}`} />
            <div className="tti-timeline__caption">
              <div className="tti-timeline__caption-l">{status === 'FAILED' ? 'failed' : 'completed'}</div>
              <div className="tti-timeline__caption-v">{fmtRelative(completed_at, n)}</div>
            </div>
          </div>
        )}
        {status === 'ACTIVE' && (
          <div className="tti-timeline__now" style={{ left: pos(n) }}>
            <div className="tti-timeline__now-line" />
            <div className="tti-timeline__now-label">now</div>
          </div>
        )}
      </div>
    </div>
  )
}

// ---- Context table ----
function ContextTable({ ctx }: { ctx: Record<string, unknown> }) {
  const entries = Object.entries(ctx || {})
  if (!entries.length) return <div className="tti-detail__empty">No context.</div>
  return (
    <div className="tti-kv">
      {entries.map(([k, v]) => (
        <div key={k} className="tti-kv__row">
          <div className="tti-kv__k">{k}</div>
          <div className="tti-kv__v">{typeof v === 'object' ? JSON.stringify(v) : String(v)}</div>
        </div>
      ))}
    </div>
  )
}

// ---- Related runs ----
function RelatedRuns({
  proc,
  onOpen,
}: {
  proc: ProcessResponse
  onOpen: (p: ProcessResponse) => void
}) {
  const n = now()
  // One bounded request for this name's newest runs instead of scanning a
  // client-side copy of the whole table
  const { data } = useProcessList({ name: proc.name, sort_by: 'started_at:desc', limit: 7 })
  const related = (data?.data ?? [])
    .filter((p) => p.id !== proc.id)
    .slice(0, 6)
  if (!related.length) return <div className="tti-detail__empty">No other runs of this process.</div>
  return (
    <div className="tti-related">
      {related.map((r) => (
        <button key={r.id} type="button" className="tti-related__row" onClick={() => onOpen(r)}>
          <StatusPill value={r.status} subtle />
          <div className="tti-related__id">{r.id}</div>
          <div className="tti-related__meta">
            <span>{fmtRelative(r.started_at, n)}</span>
            <span className="tti-related__sep">·</span>
            <span>{fmtDuration(r.duration)}</span>
          </div>
          {r.deadline_status && <StatusPill value={r.deadline_status} subtle />}
        </button>
      ))}
    </div>
  )
}

// ---- Main component ----
interface DetailPanelProps {
  proc: ProcessResponse
  onClose: () => void
  onComplete: (proc: ProcessResponse, status: 'COMPLETED' | 'FAILED') => void
  onOpenOther: (proc: ProcessResponse) => void
  onDelete?: (proc: ProcessResponse) => void
}

export function DetailPanel({ proc, onClose, onComplete, onOpenOther, onDelete }: DetailPanelProps) {
  const [tab, setTab] = useState<'timeline' | 'context' | 'tags' | 'related'>('timeline')
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [, forceUpdate] = useState(0)
  const n = now() // re-evaluated fresh on every render
  const deleteMut = useDeleteProcess()

  // Reset to the timeline tab when a different process is shown; render-phase
  // state adjustment instead of an effect (see React docs on resetting state
  // when a prop changes)
  const procKey = proc ? `${proc.name}/${proc.id}` : undefined
  const [prevProcKey, setPrevProcKey] = useState(procKey)
  if (procKey !== prevProcKey) {
    setPrevProcKey(procKey)
    setTab('timeline')
  }

  // Escape to close (or cancel confirm)
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (confirmDelete) setConfirmDelete(false)
        else onClose()
      }
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose, confirmDelete])

  // 1-second tick so "Running for X" stays live for ACTIVE processes
  useEffect(() => {
    if (proc?.status !== 'ACTIVE') return
    const id = setInterval(() => forceUpdate((c) => c + 1), 1000)
    return () => clearInterval(id)
  }, [proc?.status])

  const handleDelete = useCallback(() => {
    deleteMut.mutate(
      { name: proc.name, id: proc.id },
      {
        onSuccess: () => { onDelete?.(proc); onClose() },
      }
    )
  }, [deleteMut, proc, onDelete, onClose])

  const tabs = [
    { id: 'timeline' as const, label: 'Timeline' },
    { id: 'context' as const, label: 'Context', count: Object.keys(proc.context || {}).length },
    { id: 'tags' as const, label: 'Tags', count: proc.tags.length },
    { id: 'related' as const, label: 'Related runs' },
  ]

  return (
    <>
      <div className="tti-overlay" onClick={onClose} />
      <aside className="tti-detail" role="dialog" aria-label={`Process ${proc.id}`}>
        <header className="tti-detail__header">
          <div className="tti-detail__breadcrumb">
            <Icon name="folder" size={14} />
            <span>{proc.name}</span>
            <Icon name="chevron_right" size={12} />
            <span className="tti-detail__breadcrumb-id">{proc.id}</span>
          </div>
          <div className="tti-detail__actions">
            {proc.status === 'ACTIVE' && !confirmDelete && (
              <>
                <Button size="sm" variant="primary" icon="check" onClick={() => onComplete(proc, 'COMPLETED')}>
                  Complete
                </Button>
                <Button size="sm" variant="danger" icon="x" onClick={() => onComplete(proc, 'FAILED')}>
                  Fail
                </Button>
              </>
            )}
            {confirmDelete ? (
              <>
                <span style={{ fontSize: '12px', color: 'var(--text-2)' }}>Delete this run?</span>
                <Button size="sm" variant="danger" onClick={handleDelete} disabled={deleteMut.isPending}>
                  {deleteMut.isPending ? 'Deleting…' : 'Confirm'}
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setConfirmDelete(false)}>Cancel</Button>
              </>
            ) : (
              <button type="button" className="tti-icon-btn" onClick={() => setConfirmDelete(true)} title="Delete process">
                <Icon name="trash" size={15} />
              </button>
            )}
            <button type="button" className="tti-icon-btn" onClick={onClose} title="Close">
              <Icon name="x" size={16} />
            </button>
          </div>
        </header>

        <div className="tti-detail__hero">
          <div className="tti-detail__title-row">
            <StatusPill value={proc.status} />
            {proc.deadline_status && <StatusPill value={proc.deadline_status} subtle />}
          </div>
          <h2 className="tti-detail__id">{proc.id}</h2>
          <div className="tti-detail__name">{proc.name}</div>
          <div className="tti-detail__metrics">
            <div className="tti-metric">
              <div className="tti-metric__l">Started</div>
              <div className="tti-metric__v">{fmtRelative(proc.started_at, n)}</div>
              <div className="tti-metric__sub">{fmtAbsolute(proc.started_at)}</div>
            </div>
            <div className="tti-metric">
              <div className="tti-metric__l">{proc.completed_at ? 'Duration' : 'Running for'}</div>
              <div className="tti-metric__v">{fmtDuration(proc.duration)}</div>
              <div className="tti-metric__sub">{proc.completed_at ? 'wall clock' : 'and counting'}</div>
            </div>
            <div className="tti-metric">
              <div className="tti-metric__l">Deadline</div>
              <div className="tti-metric__v">{proc.deadline ? fmtRelative(proc.deadline, n) : '—'}</div>
              <div className="tti-metric__sub">{proc.deadline ? fmtAbsolute(proc.deadline) : 'no deadline set'}</div>
            </div>
          </div>
        </div>

        <nav className="tti-detail__tabs" role="tablist">
          {tabs.map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={tab === t.id}
              className={`tti-detail__tab${tab === t.id ? ' tti-detail__tab--active' : ''}`}
              onClick={() => setTab(t.id)}
            >
              {t.label}
              {t.count != null && <span className="tti-detail__tab-count">{t.count}</span>}
            </button>
          ))}
        </nav>

        <div className="tti-detail__body">
          {tab === 'timeline' && (
            <div className="tti-detail__section">
              <Timeline proc={proc} />
              <div className="tti-detail__events">
                <div className="tti-event">
                  <div className="tti-event__dot tti-event__dot--blue" />
                  <div className="tti-event__time">{fmtAbsolute(proc.started_at)}</div>
                  <div className="tti-event__msg">
                    Process registered via <code>POST /processes/{proc.name}</code>
                  </div>
                </div>
                {proc.deadline && (
                  <div className="tti-event">
                    <div className="tti-event__dot tti-event__dot--neutral" />
                    <div className="tti-event__time">{fmtAbsolute(proc.deadline)}</div>
                    <div className="tti-event__msg">
                      Deadline <b>{fmtRelative(proc.deadline, n)}</b>
                    </div>
                  </div>
                )}
                {proc.completed_at != null && (
                  <div className="tti-event">
                    <div className={`tti-event__dot tti-event__dot--${proc.status === 'FAILED' ? 'red' : 'green'}`} />
                    <div className="tti-event__time">{fmtAbsolute(proc.completed_at)}</div>
                    <div className="tti-event__msg">
                      Marked <b>{proc.status.toLowerCase()}</b> via{' '}
                      <code>PUT /processes/{proc.name}/{proc.id}/complete</code>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
          {tab === 'context' && (
            <div className="tti-detail__section">
              <ContextTable ctx={proc.context} />
            </div>
          )}
          {tab === 'tags' && (
            <div className="tti-detail__section">
              {proc.tags.length === 0 ? (
                <div className="tti-detail__empty">No tags.</div>
              ) : (
                <div className="tti-detail__tags">
                  {proc.tags.map((t) => (
                    <TagChip key={`${t.key}:${t.value}`} tagKey={t.key} value={t.value} />
                  ))}
                </div>
              )}
            </div>
          )}
          {tab === 'related' && (
            <div className="tti-detail__section">
              <RelatedRuns proc={proc} onOpen={onOpenOther} />
            </div>
          )}
        </div>
      </aside>
    </>
  )
}
