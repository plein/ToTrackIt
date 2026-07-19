import { useState } from 'react'
import type { NameRollupEntry, ProcessResponse } from '@/types'
import { fmtRelative, fmtDuration } from '@/lib/format'
import { Icon } from '@/components/Icon'
import { StatusPill } from '@/components/StatusPill'
import { STATUS_TONE } from '@/lib/statusTone'
import { useNameRollups, useProcessList } from '@/hooks/useProcesses'

const now = () => Math.floor(Date.now() / 1000)

const ROLLUP_PAGE_SIZE = 20

interface NameRollupsProps {
  onPickName: (name: string) => void
  onOpenProcess: (p: ProcessResponse) => void
}

// One rollup card. Counts come from the aggregated /analytics/names endpoint;
// the 8-run status strip and "last run" row are a single bounded per-name
// request instead of an upfront crawl of every process.
function RollupCard({
  rollup,
  onPickName,
  onOpenProcess,
}: {
  rollup: NameRollupEntry
  onPickName: (name: string) => void
  onOpenProcess: (p: ProcessResponse) => void
}) {
  const n = now()
  const { data: recent } = useProcessList({ name: rollup.name, sort_by: 'started_at:desc', limit: 8 })
  const runs = recent?.data ?? []
  const last = runs[0]
  const strip = runs.slice(0, 8).reverse().map((r) => STATUS_TONE[r.deadline_status ?? ''] || 'neutral')

  const missed = rollup.overdue + rollup.completed_late
  const onTimeRate = rollup.completed > 0
    ? Math.round((rollup.completed_on_time / rollup.completed) * 100)
    : null

  return (
    <div className="tti-rollup">
      <div className="tti-rollup__head">
        <button type="button" className="tti-rollup__name" onClick={() => onPickName(rollup.name)}>
          <Icon name="folder" size={14} />
          <span>{rollup.name}</span>
        </button>
        <div className="tti-rollup__counts">
          <span className="tti-rollup__count"><b>{rollup.total}</b> runs</span>
          {rollup.active > 0 && <StatusPill value="ACTIVE" />}
          {missed > 0 && (
            <span className="tti-pill tti-pill--red">
              <span className="tti-dot tti-dot--red" />
              {missed} missed
            </span>
          )}
          {rollup.failed > 0 && (
            <span className="tti-pill tti-pill--amber">
              <span className="tti-dot tti-dot--amber" />
              {rollup.failed} failed
            </span>
          )}
          {onTimeRate != null && (
            <span className="tti-rollup__rate" title="Completed on time">
              <b>{onTimeRate}%</b> on-time
            </span>
          )}
        </div>
      </div>
      <div className="tti-rollup__body">
        <div className="tti-rollup__strip" aria-label="Recent runs">
          {strip.map((tone, i) => (
            <span key={i} className={`tti-rollup__cell tti-rollup__cell--${tone}`} />
          ))}
          {Array.from({ length: Math.max(0, 8 - strip.length) }).map((_, i) => (
            <span key={`e${i}`} className="tti-rollup__cell tti-rollup__cell--empty" />
          ))}
        </div>
        {last ? (
          <button
            type="button"
            className="tti-rollup__last"
            onClick={() => onOpenProcess(last)}
            title="Open most recent run"
          >
            <span className="tti-rollup__last-l">last run</span>
            <span className="tti-rollup__last-id">{last.id}</span>
            <span className="tti-rollup__last-meta">
              {fmtRelative(last.started_at, n)} · {fmtDuration(last.duration)}
            </span>
            <Icon name="chevron_right" size={14} />
          </button>
        ) : (
          <span className="tti-rollup__last-meta">
            {rollup.last_started_at != null ? `last run ${fmtRelative(rollup.last_started_at, n)}` : ''}
          </span>
        )}
        <button type="button" className="tti-btn tti-btn--ghost tti-btn--sm" onClick={() => onPickName(rollup.name)}>
          View all {rollup.total} <Icon name="arrow_right" size={12} />
        </button>
      </div>
    </div>
  )
}

export function NameRollups({ onPickName, onOpenProcess }: NameRollupsProps) {
  const [page, setPage] = useState(0)
  const { data } = useNameRollups(ROLLUP_PAGE_SIZE, page * ROLLUP_PAGE_SIZE)
  const rollups = data?.data ?? []
  const total = data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / ROLLUP_PAGE_SIZE))

  return (
    <div className="tti-rollups">
      {rollups.map((g) => (
        <RollupCard key={g.name} rollup={g} onPickName={onPickName} onOpenProcess={onOpenProcess} />
      ))}
      {total > ROLLUP_PAGE_SIZE && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '12px', color: 'var(--text-3)' }}>
            {page * ROLLUP_PAGE_SIZE + 1}–{Math.min((page + 1) * ROLLUP_PAGE_SIZE, total)} of {total} names
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
