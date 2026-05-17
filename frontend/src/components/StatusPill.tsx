import type { ProcessStatus, DeadlineStatus } from '@/types'

type StatusValue = ProcessStatus | DeadlineStatus | string

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Active',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  ON_TRACK: 'On track',
  MISSED: 'Missed',
  COMPLETED_ON_TIME: 'On time',
  COMPLETED_LATE: 'Late',
}

export const STATUS_TONE: Record<string, string> = {
  ACTIVE: 'blue',
  COMPLETED: 'green',
  FAILED: 'red',
  ON_TRACK: 'green',
  MISSED: 'red',
  COMPLETED_ON_TIME: 'green',
  COMPLETED_LATE: 'amber',
}

interface StatusDotProps {
  tone?: string
  pulse?: boolean
}

export function StatusDot({ tone = 'neutral', pulse = false }: StatusDotProps) {
  return (
    <span
      className={`tti-dot tti-dot--${tone}${pulse ? ' tti-dot--pulse' : ''}`}
      aria-hidden="true"
    />
  )
}

interface StatusPillProps {
  value: StatusValue
  subtle?: boolean
}

export function StatusPill({ value, subtle = false }: StatusPillProps) {
  const tone = STATUS_TONE[value] || 'neutral'
  const label = STATUS_LABEL[value] || value
  return (
    <span className={`tti-pill tti-pill--${tone}${subtle ? ' tti-pill--subtle' : ''}`}>
      <StatusDot tone={tone} pulse={value === 'ACTIVE' || value === 'ON_TRACK'} />
      {label}
    </span>
  )
}
