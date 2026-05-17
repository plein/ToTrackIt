export function fmtRelative(ts: number, ref = Math.floor(Date.now() / 1000)): string {
  const diff = ref - ts
  const abs = Math.abs(diff)
  const ago = diff >= 0
  let val: number, unit: string
  if (abs < 60) { val = abs; unit = 's' }
  else if (abs < 3600) { val = Math.floor(abs / 60); unit = 'm' }
  else if (abs < 86400) { val = Math.floor(abs / 3600); unit = 'h' }
  else { val = Math.floor(abs / 86400); unit = 'd' }
  return ago ? `${val}${unit} ago` : `in ${val}${unit}`
}

export function fmtDuration(secs: number | null | undefined): string {
  if (secs == null) return '—'
  if (secs < 60) return `${secs}s`
  if (secs < 3600) {
    const m = Math.floor(secs / 60)
    const s = secs % 60
    return s ? `${m}m ${s}s` : `${m}m`
  }
  if (secs < 86400) {
    const h = Math.floor(secs / 3600)
    const m = Math.floor((secs % 3600) / 60)
    return m ? `${h}h ${m}m` : `${h}h`
  }
  const d = Math.floor(secs / 86400)
  const h = Math.floor((secs % 86400) / 3600)
  return h ? `${d}d ${h}h` : `${d}d`
}

export function fmtAbsolute(ts: number | null | undefined): string {
  if (ts == null) return '—'
  const d = new Date(ts * 1000)
  const yyyy = d.getUTCFullYear()
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0')
  const dd = String(d.getUTCDate()).padStart(2, '0')
  const hh = String(d.getUTCHours()).padStart(2, '0')
  const mi = String(d.getUTCMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi} UTC`
}

// Legacy aliases kept for transition
export const formatTs = fmtAbsolute
export const formatDuration = fmtDuration
