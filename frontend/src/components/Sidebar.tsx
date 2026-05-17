import { Icon } from './Icon'
import { Wordmark } from './Wordmark'

interface SidebarCounts {
  total: number
  active: number
  missed: number
  completed: number
}

type NavId = 'processes' | 'active' | 'missed' | 'completed' | 'names' | 'metrics' | 'tags' | 'settings'

interface SidebarProps {
  active: NavId
  onNav: (id: NavId) => void
  counts: SidebarCounts
}

export function Sidebar({ active, onNav, counts }: SidebarProps) {
  const views = [
    { id: 'processes' as NavId, label: 'Processes', icon: 'list', count: counts.total },
    { id: 'active' as NavId,    label: 'Active',    icon: 'activity', count: counts.active },
    { id: 'missed' as NavId,    label: 'Missed',    icon: 'alert', count: counts.missed, tone: 'red' },
    { id: 'completed' as NavId, label: 'Completed', icon: 'check', count: counts.completed },
    { id: 'names' as NavId,     label: 'By name',   icon: 'layers' },
  ]
  const workspace = [
    { id: 'metrics' as NavId,  label: 'Metrics',  icon: 'chart' },
    { id: 'tags' as NavId,     label: 'Tags',     icon: 'tag' },
    { id: 'settings' as NavId, label: 'Settings', icon: 'settings' },
  ]

  return (
    <aside className="tti-sidebar">
      <div className="tti-sidebar__top">
        <Wordmark />
        <button type="button" className="tti-icon-btn tti-icon-btn--quiet" title="Notifications">
          <Icon name="bell" size={16} />
        </button>
      </div>
      <div className="tti-sidebar__workspace">
        <div className="tti-sidebar__ws-dot" style={{ background: 'var(--accent)' }} />
        <div className="tti-sidebar__ws-name">my-workspace</div>
        <Icon name="chevron_down" size={14} />
      </div>
      <div className="tti-sidebar__section">
        <div className="tti-sidebar__heading">Views</div>
        {views.map((it) => (
          <button
            key={it.id}
            type="button"
            className={`tti-nav${active === it.id ? ' tti-nav--active' : ''}`}
            onClick={() => onNav(it.id)}
          >
            <Icon name={it.icon as Parameters<typeof Icon>[0]['name']} size={16} />
            <span className="tti-nav__label">{it.label}</span>
            {it.count != null && (
              <span className={`tti-nav__count${it.tone ? ` tti-nav__count--${it.tone}` : ''}`}>
                {it.count}
              </span>
            )}
          </button>
        ))}
      </div>
      <div className="tti-sidebar__section">
        <div className="tti-sidebar__heading">Workspace</div>
        {workspace.map((it) => (
          <button
            key={it.id}
            type="button"
            className={`tti-nav${active === it.id ? ' tti-nav--active' : ''}`}
            onClick={() => onNav(it.id)}
          >
            <Icon name={it.icon as Parameters<typeof Icon>[0]['name']} size={16} />
            <span className="tti-nav__label">{it.label}</span>
          </button>
        ))}
      </div>
      <div className="tti-sidebar__spacer" />
      <a className="tti-sidebar__docs" href="http://localhost:8081" target="_blank" rel="noreferrer">
        <Icon name="book" size={14} />
        <span>API reference</span>
        <span className="tti-sidebar__docs-host">localhost:8081</span>
      </a>
      <div className="tti-sidebar__user">
        <div className="tti-avatar">ME</div>
        <div className="tti-sidebar__user-meta">
          <div className="tti-sidebar__user-name">You</div>
          <div className="tti-sidebar__user-role">Operator</div>
        </div>
      </div>
    </aside>
  )
}
