import type { ProcessTag, TagImpactEntry } from '@/types'
import { Icon } from '@/components/Icon'
import { TagChip } from '@/components/TagChip'

// Server-side aggregation of deadline outcomes per tag: when things go wrong,
// this shows whether the problems share a segment (country, locale, provider…)
// so the on-call landing from an alert can see the blast radius at a glance.
interface ImpactedTagsProps {
  entries: TagImpactEntry[]
  windowHours: number
  activeTags: ProcessTag[]
  onToggleTag: (t: ProcessTag) => void
}

export function ImpactedTags({ entries, windowHours, activeTags, onToggleTag }: ImpactedTagsProps) {
  const impacted = entries.filter((e) => e.problems > 0).slice(0, 8)
  if (impacted.length === 0) return null
  const maxProblems = impacted[0].problems

  return (
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
          <Icon name="alert" size={13} /> Impacted tags
        </span>
        <span style={{ fontSize: '11px', color: 'var(--text-3)' }}>
          overdue, late, and failed processes · last {windowHours}h · click a tag to filter
        </span>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        {impacted.map((e) => (
          <div key={`${e.key}:${e.value}`} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ minWidth: '180px', display: 'flex' }}>
              <TagChip
                tagKey={e.key}
                value={e.value}
                onClick={() => onToggleTag({ key: e.key, value: e.value })}
                active={activeTags.some((t) => t.key === e.key && t.value === e.value)}
              />
            </div>
            <div
              aria-hidden="true"
              style={{ flex: 1, height: '6px', borderRadius: '3px', background: 'var(--surface-2)', overflow: 'hidden' }}
            >
              <div
                style={{
                  width: `${Math.max(4, Math.round((e.problems / maxProblems) * 100))}%`,
                  height: '100%',
                  borderRadius: '3px',
                  background: e.overdue > 0 ? 'var(--red, #d64545)' : 'var(--amber, #d69e2e)',
                }}
              />
            </div>
            <div style={{ fontSize: '11px', color: 'var(--text-2)', whiteSpace: 'nowrap', minWidth: '190px', textAlign: 'right' }}>
              {e.overdue > 0 && <span style={{ color: 'var(--red, #d64545)', fontWeight: 600 }}>{e.overdue} overdue</span>}
              {e.overdue > 0 && (e.completed_late > 0 || e.failed > 0) && ' · '}
              {e.completed_late > 0 && <span style={{ color: 'var(--amber, #d69e2e)' }}>{e.completed_late} late</span>}
              {e.completed_late > 0 && e.failed > 0 && ' · '}
              {e.failed > 0 && <span>{e.failed} failed</span>}
              <span style={{ color: 'var(--text-3)' }}> / {e.total} total</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
