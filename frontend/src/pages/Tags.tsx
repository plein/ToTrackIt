import { useMemo } from 'react'
import { TagChip } from '@/components/TagChip'
import { useTagImpact } from '@/hooks/useProcesses'

interface TagsPageProps {
  onFilterTag: (key: string, value: string) => void
}

// Tag catalogue built from the aggregated /analytics/tags endpoint (top 100
// tag pairs by impact over the last 30 days) instead of crawling every process.
export function Tags({ onFilterTag }: TagsPageProps) {
  const { data: tagImpact } = useTagImpact(undefined, 720)

  // Group by key → values with counts
  const groups = useMemo(() => {
    const m = new Map<string, Map<string, number>>()
    for (const t of tagImpact?.tags ?? []) {
      if (!m.has(t.key)) m.set(t.key, new Map())
      m.get(t.key)!.set(t.value, t.total)
    }
    return [...m.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([key, vals]) => ({
        key,
        values: [...vals.entries()].sort((a, b) => b[1] - a[1]),
        total: [...vals.values()].reduce((s, n) => s + n, 0),
      }))
  }, [tagImpact])

  const totalUnique = groups.reduce((s, g) => s + g.values.length, 0)

  return (
    <div className="tti-dashboard">
      <div className="tti-page-header">
        <div>
          <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
          <h1 className="tti-page-header__title">Tags</h1>
          <div className="tti-page-header__sub">
            {totalUnique} tag values across {groups.length} keys, last 30 days. Click any tag to filter the process list.
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
