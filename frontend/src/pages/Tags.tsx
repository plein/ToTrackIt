import { useMemo } from 'react'
import type { ProcessResponse } from '@/types'
import { TagChip } from '@/components/TagChip'

interface TagsPageProps {
  processes: ProcessResponse[]
  onFilterTag: (key: string, value: string) => void
}

export function Tags({ processes, onFilterTag }: TagsPageProps) {
  // Group by key → values with counts
  const groups = useMemo(() => {
    const m = new Map<string, Map<string, number>>()
    for (const p of processes) {
      for (const t of p.tags) {
        if (!m.has(t.key)) m.set(t.key, new Map())
        const vals = m.get(t.key)!
        vals.set(t.value, (vals.get(t.value) ?? 0) + 1)
      }
    }
    return [...m.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([key, vals]) => ({
        key,
        values: [...vals.entries()].sort((a, b) => b[1] - a[1]),
        total: [...vals.values()].reduce((s, n) => s + n, 0),
      }))
  }, [processes])

  const totalUnique = groups.reduce((s, g) => s + g.values.length, 0)

  return (
    <div className="tti-dashboard">
      <div className="tti-page-header">
        <div>
          <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
          <h1 className="tti-page-header__title">Tags</h1>
          <div className="tti-page-header__sub">
            {totalUnique} unique tag values across {groups.length} keys. Click any tag to filter the process list.
          </div>
        </div>
      </div>

      {groups.length === 0 ? (
        <div className="tti-empty">
          <div className="tti-empty__title">No tags yet</div>
          <div className="tti-empty__sub">
            Add tags when registering a process to filter and group runs later.
          </div>
        </div>
      ) : (
        <div className="tti-tags-grid">
          {groups.map((g) => (
            <div key={g.key} className="tti-tag-group">
              <div className="tti-tag-group__key">{g.key} · {g.total}</div>
              <div className="tti-tag-group__values">
                {g.values.map(([value, count]) => (
                  <div key={value} style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <TagChip
                      tagKey={g.key}
                      value={value}
                      onClick={() => onFilterTag(g.key, value)}
                    />
                    <span style={{ fontSize: '11px', color: 'var(--text-3)' }}>{count}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
