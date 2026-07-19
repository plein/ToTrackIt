import { useState, useMemo, useCallback, useEffect } from 'react'
import type { ProcessResponse } from '@/types'
import { useSummary, useNameRollups, useCreateProcess, useCompleteProcess } from '@/hooks/useProcesses'
import { getProcess } from '@/api/processes'
import { Sidebar } from '@/components/Sidebar'
import { DetailPanel } from '@/components/DetailPanel'
import { CreateProcessModal } from '@/components/CreateProcessModal'
import { Toast } from '@/components/Toast'
import { Icon } from '@/components/Icon'
import { Button } from '@/components/Button'
import { Dashboard } from '@/pages/Dashboard'
import { NameRollups } from '@/pages/NameRollups'
import { ProcessName } from '@/pages/ProcessName'
import { Tags } from '@/pages/Tags'
import { Metrics } from '@/pages/Metrics'

type NavId = 'processes' | 'active' | 'missed' | 'completed' | 'names' | 'metrics' | 'tags' | 'settings'

interface ToastState {
  msg: string
  tone: 'neutral' | 'green' | 'red'
}

// ---- Loading spinner ----
function LoadingScreen() {
  return (
    <div className="tti-load-center">
      <div className="tti-spinner" />
      <span>Loading processes…</span>
    </div>
  )
}

// ---- Error state ----
function ErrorScreen({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="tti-dashboard">
      <div style={{ padding: '40px 0 0' }}>
        <div className="tti-error-box">
          <div className="tti-error-box__icon"><Icon name="alert" size={18} /></div>
          <div>
            <div className="tti-error-box__title">Cannot reach the backend</div>
            <div className="tti-error-box__msg">
              Make sure the API server is running on <code>localhost:8080</code> and try again.
            </div>
            <Button variant="default" icon="refresh" onClick={onRetry}>Retry</Button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function App() {
  const [activeNav, setActiveNav] = useState<NavId>('processes')
  // Per-name view, addressable as /?name=<process-name>
  const [nameView, setNameView] = useState<string | null>(
    () => new URLSearchParams(window.location.search).get('name'),
  )
  const [nameJump, setNameJump] = useState<string | null>(null)
  // null = no filter; {key,value} = tag filter applied from Tags page
  const [tagJump, setTagJump] = useState<{ key: string; value: string } | null>(null)
  const [openProc, setOpenProc] = useState<ProcessResponse | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const [theme, setTheme] = useState<'light' | 'dark'>('light')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
  }, [theme])

  // Headline counts come from one aggregate endpoint instead of crawling the
  // process list; it also doubles as the backend-reachability probe.
  const { data: summary, isLoading, isError, refetch } = useSummary()
  const { data: rollups } = useNameRollups(100, 0)

  // Deep link from alerts: /?process=<name>/<id> opens the process detail panel
  // (this is the URL embedded in deadline-missed webhook payloads).
  useEffect(() => {
    const deepLink = new URLSearchParams(window.location.search).get('process')
    if (!deepLink) return
    const slash = deepLink.indexOf('/')
    if (slash <= 0) return
    const name = deepLink.slice(0, slash)
    const id = deepLink.slice(slash + 1)
    let cancelled = false
    getProcess(name, id)
      .then((proc) => { if (!cancelled) setOpenProc(proc) })
      .catch(() => { /* stale link; nothing to open */ })
    return () => { cancelled = true }
  }, [])

  const counts = useMemo(() => ({
    total:     summary?.total ?? 0,
    active:    summary?.active ?? 0,
    missed:    summary?.overdue ?? 0,
    completed: summary?.completed ?? 0,
  }), [summary])

  const suggestedNames = useMemo(
    () => (rollups?.data ?? []).map((r) => r.name),
    [rollups],
  )

  const flash = useCallback((msg: string, tone: ToastState['tone'] = 'neutral') => {
    setToast({ msg, tone })
    setTimeout(() => setToast(null), 2400)
  }, [])

  const createMut = useCreateProcess()
  const completeMut = useCompleteProcess()

  const handleCreate = useCallback((createData: {
    name: string
    id: string
    deadline_secs: number | null
    tags: { key: string; value: string }[]
    context: Record<string, unknown>
  }) => {
    const deadline = createData.deadline_secs
      ? Math.floor(Date.now() / 1000) + createData.deadline_secs
      : undefined
    createMut.mutate(
      { name: createData.name, body: { id: createData.id, deadline, tags: createData.tags, context: createData.context } },
      {
        onSuccess: (proc) => { setShowCreate(false); flash(`Registered ${createData.id}`, 'green'); setOpenProc(proc) },
        onError: (err: unknown) => flash((err as { message?: string })?.message ?? 'Failed to create process', 'red'),
      }
    )
  }, [createMut, flash])

  const handleComplete = useCallback((proc: ProcessResponse, status: 'COMPLETED' | 'FAILED') => {
    completeMut.mutate(
      { name: proc.name, id: proc.id, body: { status } },
      {
        onSuccess: (updated) => {
          if (openProc?.id === proc.id && openProc?.name === proc.name) setOpenProc(updated)
          flash(`Marked ${proc.id} as ${status.toLowerCase()}`, status === 'FAILED' ? 'red' : 'green')
        },
        onError: (err: unknown) => flash((err as { message?: string })?.message ?? 'Failed to update process', 'red'),
      }
    )
  }, [completeMut, openProc, flash])

  const openNameView = useCallback((name: string) => {
    setNameView(name)
    const url = new URL(window.location.href)
    url.searchParams.set('name', name)
    url.searchParams.delete('process')
    window.history.pushState({}, '', url)
  }, [])

  const closeNameView = useCallback(() => {
    setNameView(null)
    const url = new URL(window.location.href)
    url.searchParams.delete('name')
    window.history.pushState({}, '', url)
  }, [])

  // Keep the view in sync with browser back/forward
  useEffect(() => {
    const onPop = () => setNameView(new URLSearchParams(window.location.search).get('name'))
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  const handleNav = (id: NavId) => {
    setActiveNav(id)
    if (nameView) closeNameView()
    setNameJump(null)
    setTagJump(null)
  }

  // Navigate to Processes view pre-filtered by a tag from the Tags page
  const handleFilterTag = (key: string, value: string) => {
    setActiveNav('processes')
    if (nameView) closeNameView()
    setNameJump(null)
    setTagJump({ key, value })
  }

  // Sidebar views map to server-side filter presets on the Dashboard
  const navPreset = useMemo(() => {
    switch (activeNav) {
      case 'active':    return { status: 'ACTIVE' as const }
      case 'missed':    return { deadline: 'MISSED' as const }
      case 'completed': return { status: 'COMPLETED' as const }
      default:          return {}
    }
  }, [activeNav])

  return (
    <div className="tti-app">
      <Sidebar active={activeNav} onNav={handleNav} counts={counts} />
      <main className="tti-main">
        {isLoading ? (
          <LoadingScreen />
        ) : isError ? (
          <ErrorScreen onRetry={() => refetch()} />
        ) : nameView ? (
          <ProcessName
            name={nameView}
            onBack={closeNameView}
            onOpenProcess={setOpenProc}
            onComplete={handleComplete}
          />
        ) : activeNav === 'names' ? (
          <div className="tti-dashboard">
            <div className="tti-page-header">
              <div>
                <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
                <h1 className="tti-page-header__title">By name</h1>
                <div className="tti-page-header__sub">
                  Each process name groups related runs. Open one to filter to all its runs.
                </div>
              </div>
            </div>
            <NameRollups
              onPickName={openNameView}
              onOpenProcess={setOpenProc}
            />
          </div>
        ) : activeNav === 'metrics' ? (
          <Metrics />
        ) : activeNav === 'tags' ? (
          <Tags onFilterTag={handleFilterTag} />
        ) : activeNav === 'settings' ? (
          <div className="tti-dashboard">
            <div className="tti-page-header">
              <div>
                <div className="tti-page-header__eyebrow">Workspace · my-workspace</div>
                <h1 className="tti-page-header__title">Settings</h1>
                <div className="tti-page-header__sub">Appearance preferences.</div>
              </div>
            </div>
            <div style={{ marginTop: '4px' }}>
              <div style={{ fontSize: '13px', fontWeight: 500, color: 'var(--text-2)', marginBottom: '10px' }}>Theme</div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button type="button" className={`tti-chip${theme === 'light' ? ' tti-chip--active' : ''}`} onClick={() => setTheme('light')}>Light</button>
                <button type="button" className={`tti-chip${theme === 'dark' ? ' tti-chip--active' : ''}`} onClick={() => setTheme('dark')}>Dark</button>
              </div>
            </div>
          </div>
        ) : (
          <Dashboard
            onOpenProcess={setOpenProc}
            onOpenCreate={() => setShowCreate(true)}
            onComplete={handleComplete}
            onOpenName={openNameView}
            initialNameFilter={nameJump}
            initialTagFilter={tagJump}
            initialStatusFilter={navPreset.status ?? null}
            initialDeadlineFilter={navPreset.deadline ?? null}
            navKey={activeNav + (nameJump || '') + (tagJump ? `${tagJump.key}:${tagJump.value}` : '')}
          />
        )}
      </main>

      {openProc && (
        <DetailPanel
          proc={openProc}
          onClose={() => setOpenProc(null)}
          onComplete={handleComplete}
          onOpenOther={setOpenProc}
          onDelete={(proc) => flash(`Deleted ${proc.id}`, 'neutral')}
        />
      )}

      {showCreate && (
        <CreateProcessModal
          onClose={() => setShowCreate(false)}
          onCreate={handleCreate}
          suggestedNames={suggestedNames}
        />
      )}

      <Toast msg={toast?.msg} tone={toast?.tone} />
    </div>
  )
}
