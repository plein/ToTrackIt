import type { ProcessResponse } from '@/types'
import { STATUS_TONE } from './StatusPill'

interface DeadlineBarProps {
  proc: ProcessResponse
  now: number
}

export function DeadlineBar({ proc, now }: DeadlineBarProps) {
  const { started_at, deadline, completed_at, deadline_status } = proc
  if (!deadline) {
    return <span className="tti-deadline-bar tti-deadline-bar--none">no deadline</span>
  }
  const total = deadline - started_at
  const ref = completed_at != null ? completed_at : now
  const elapsed = Math.max(0, ref - started_at)
  const pct = Math.min(120, (elapsed / total) * 100)
  const tone = STATUS_TONE[deadline_status ?? ''] || 'neutral'
  const overflow = pct > 100
  return (
    <div
      className={`tti-deadline-bar tti-deadline-bar--${tone}`}
      title={`${Math.round(pct)}% of deadline used`}
    >
      <div className="tti-deadline-bar__track">
        <div className="tti-deadline-bar__fill" style={{ width: `${Math.min(100, pct)}%` }} />
        {overflow && (
          <div className="tti-deadline-bar__over" style={{ width: `${Math.min(20, pct - 100)}%` }} />
        )}
        <div className="tti-deadline-bar__deadline-tick" />
      </div>
    </div>
  )
}
