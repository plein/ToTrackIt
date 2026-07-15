import { useMemo } from 'react'
import type { ProcessResponse } from '@/types'
import { fmtRelative, fmtDuration } from '@/lib/format'
import { Icon } from '@/components/Icon'
import { StatusPill } from '@/components/StatusPill'
import { STATUS_TONE } from '@/lib/statusTone'

const now = () => Math.floor(Date.now() / 1000)

interface NameRollupsProps {
  processes: ProcessResponse[]
  onPickName: (name: string) => void
  onOpenProcess: (p: ProcessResponse) => void
}

export function NameRollups({ processes, onPickName, onOpenProcess }: NameRollupsProps) {
  const n = now()

  const groups = useMemo(() => {
    const m = new Map<string, ProcessResponse[]>()
    processes.forEach((p) => {
      if (!m.has(p.name)) m.set(p.name, [])
      m.get(p.name)!.push(p)
    })
    return [...m.entries()]
      .map(([name, runs]) => {
        runs.sort((a, b) => b.started_at - a.started_at)
        const active = runs.filter((r) => r.status === 'ACTIVE').length
        const failed = runs.filter((r) => r.status === 'FAILED').length
        const missed = runs.filter((r) => r.deadline_status === 'MISSED' || r.deadline_status === 'COMPLETED_LATE').length
        const completed = runs.filter((r) => r.status === 'COMPLETED')
        const onTime = completed.filter((r) => r.deadline_status === 'COMPLETED_ON_TIME').length
        const onTimeRate = completed.length ? Math.round((onTime / completed.length) * 100) : null
        const last = runs[0]
        const strip = runs.slice(0, 8).reverse().map((r) => STATUS_TONE[r.deadline_status ?? ''] || 'neutral')
        return { name, runs, active, failed, missed, onTimeRate, last, strip }
      })
      .sort((a, b) => b.runs.length - a.runs.length)
  }, [processes])

  return (
    <div className="tti-rollups">
      {groups.map((g) => (
        <div key={g.name} className="tti-rollup">
          <div className="tti-rollup__head">
            <button type="button" className="tti-rollup__name" onClick={() => onPickName(g.name)}>
              <Icon name="folder" size={14} />
              <span>{g.name}</span>
            </button>
            <div className="tti-rollup__counts">
              <span className="tti-rollup__count"><b>{g.runs.length}</b> runs</span>
              {g.active > 0 && <StatusPill value="ACTIVE" />}
              {g.missed > 0 && (
                <span className="tti-pill tti-pill--red">
                  <span className="tti-dot tti-dot--red" />
                  {g.missed} missed
                </span>
              )}
              {g.failed > 0 && (
                <span className="tti-pill tti-pill--amber">
                  <span className="tti-dot tti-dot--amber" />
                  {g.failed} failed
                </span>
              )}
              {g.onTimeRate != null && (
                <span className="tti-rollup__rate" title="Completed on time">
                  <b>{g.onTimeRate}%</b> on-time
                </span>
              )}
            </div>
          </div>
          <div className="tti-rollup__body">
            <div className="tti-rollup__strip" aria-label="Recent runs">
              {g.strip.map((tone, i) => (
                <span key={i} className={`tti-rollup__cell tti-rollup__cell--${tone}`} />
              ))}
              {Array.from({ length: Math.max(0, 8 - g.strip.length) }).map((_, i) => (
                <span key={`e${i}`} className="tti-rollup__cell tti-rollup__cell--empty" />
              ))}
            </div>
            <button
              type="button"
              className="tti-rollup__last"
              onClick={() => onOpenProcess(g.last)}
              title="Open most recent run"
            >
              <span className="tti-rollup__last-l">last run</span>
              <span className="tti-rollup__last-id">{g.last.id}</span>
              <span className="tti-rollup__last-meta">
                {fmtRelative(g.last.started_at, n)} · {fmtDuration(g.last.duration)}
              </span>
              <Icon name="chevron_right" size={14} />
            </button>
            <button type="button" className="tti-btn tti-btn--ghost tti-btn--sm" onClick={() => onPickName(g.name)}>
              View all {g.runs.length} <Icon name="arrow_right" size={12} />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
