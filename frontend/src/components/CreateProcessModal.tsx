import { useState, useEffect } from 'react'
import { Icon } from './Icon'
import { Button } from './Button'

interface CreateData {
  name: string
  id: string
  deadline_secs: number | null
  tags: { key: string; value: string }[]
  context: Record<string, unknown>
}

interface CreateProcessModalProps {
  onClose: () => void
  onCreate: (data: CreateData) => void
  suggestedNames: string[]
}

export function CreateProcessModal({ onClose, onCreate, suggestedNames }: CreateProcessModalProps) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose])

  const [name, setName] = useState('')
  const [id, setId] = useState('')
  const [deadlineMins, setDeadlineMins] = useState(60)
  const [hasDeadline, setHasDeadline] = useState(true)
  const [tags, setTags] = useState([{ key: 'env', value: 'prod' }])
  const [contextRaw, setContextRaw] = useState('{\n  "user_id": "u_42"\n}')

  const addTag = () => setTags((cur) => [...cur, { key: '', value: '' }])
  const updateTag = (i: number, k: string, v: string) =>
    setTags((cur) => cur.map((t, j) => (j === i ? { key: k, value: v } : t)))
  const removeTag = (i: number) => setTags((cur) => cur.filter((_, j) => j !== i))

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim() || !id.trim()) return
    let context: Record<string, unknown> = {}
    try { context = JSON.parse(contextRaw) } catch { /* invalid json — ignore */ }
    onCreate({
      name: name.trim(),
      id: id.trim(),
      deadline_secs: hasDeadline ? deadlineMins * 60 : null,
      tags: tags.filter((t) => t.key && t.value),
      context,
    })
  }

  const presets = [15, 30, 60, 120, 360]

  return (
    <>
      <div className="tti-overlay tti-overlay--modal" onClick={onClose} />
      <div className="tti-modal" role="dialog" aria-label="New process">
        <form onSubmit={submit}>
          <header className="tti-modal__header">
            <div>
              <div className="tti-modal__eyebrow">Register</div>
              <h2 className="tti-modal__title">New process</h2>
            </div>
            <button type="button" className="tti-icon-btn" onClick={onClose} aria-label="Close">
              <Icon name="x" size={16} />
            </button>
          </header>

          <div className="tti-modal__body">
            <div className="tti-field">
              <label className="tti-field__label">Name</label>
              <div className="tti-field__hint">Category. Alphanumeric, _ or -.</div>
              <input
                className="tti-input tti-input--mono"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="etl-orders-daily"
                required
                pattern="[A-Za-z0-9_-]+"
                list="tti-name-suggestions"
              />
              <datalist id="tti-name-suggestions">
                {suggestedNames.map((n) => <option key={n} value={n} />)}
              </datalist>
            </div>

            <div className="tti-field">
              <label className="tti-field__label">ID</label>
              <div className="tti-field__hint">Caller-assigned. 3–50 chars, unique per name.</div>
              <input
                className="tti-input tti-input--mono"
                value={id}
                onChange={(e) => setId(e.target.value)}
                placeholder="run-2026-05-10-a31f"
                minLength={3}
                maxLength={50}
                required
              />
            </div>

            <div className="tti-field">
              <div className="tti-field__row">
                <label className="tti-field__label">Deadline</label>
                <label className="tti-toggle">
                  <input
                    type="checkbox"
                    checked={hasDeadline}
                    onChange={(e) => setHasDeadline(e.target.checked)}
                  />
                  <span className="tti-toggle__track"><span className="tti-toggle__dot" /></span>
                  <span className="tti-toggle__label">{hasDeadline ? 'On' : 'No deadline'}</span>
                </label>
              </div>
              {hasDeadline && (
                <div className="tti-deadline-picker">
                  <input
                    type="range"
                    min="5"
                    max="720"
                    step="5"
                    value={deadlineMins}
                    onChange={(e) => setDeadlineMins(parseInt(e.target.value))}
                    className="tti-range"
                  />
                  <div className="tti-deadline-picker__value">
                    <b>{deadlineMins < 60 ? `${deadlineMins}m` : `${(deadlineMins / 60).toFixed(deadlineMins % 60 ? 1 : 0)}h`}</b>
                    <span> from start</span>
                  </div>
                  <div className="tti-deadline-picker__presets">
                    {presets.map((m) => (
                      <button
                        key={m}
                        type="button"
                        className={`tti-chip${deadlineMins === m ? ' tti-chip--active' : ''}`}
                        onClick={() => setDeadlineMins(m)}
                      >
                        {m < 60 ? `${m}m` : `${m / 60}h`}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="tti-field">
              <label className="tti-field__label">Tags</label>
              <div className="tti-field__hint">Filter by these later (e.g. env:prod).</div>
              <div className="tti-tags-edit">
                {tags.map((t, i) => (
                  <div key={i} className="tti-tags-edit__row">
                    <input
                      className="tti-input tti-input--mono"
                      placeholder="key"
                      value={t.key}
                      onChange={(e) => updateTag(i, e.target.value, t.value)}
                    />
                    <span className="tti-tags-edit__sep">:</span>
                    <input
                      className="tti-input tti-input--mono"
                      placeholder="value"
                      value={t.value}
                      onChange={(e) => updateTag(i, t.key, e.target.value)}
                    />
                    <button type="button" className="tti-icon-btn" onClick={() => removeTag(i)} aria-label="Remove tag">
                      <Icon name="x" size={14} />
                    </button>
                  </div>
                ))}
                <button type="button" className="tti-link-btn" onClick={addTag}>
                  <Icon name="plus" size={12} /> add tag
                </button>
              </div>
            </div>

            <div className="tti-field">
              <label className="tti-field__label">Context (JSON)</label>
              <div className="tti-field__hint">Free-form metadata. Shows in detail panel.</div>
              <textarea
                className="tti-input tti-input--mono tti-textarea"
                rows={5}
                value={contextRaw}
                onChange={(e) => setContextRaw(e.target.value)}
              />
            </div>
          </div>

          <footer className="tti-modal__footer">
            <Button variant="ghost" onClick={onClose}>Cancel</Button>
            <Button variant="primary" type="submit" iconAfter="arrow_right">Register process</Button>
          </footer>
        </form>
      </div>
    </>
  )
}
