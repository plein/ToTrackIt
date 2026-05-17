interface ToastProps {
  msg?: string | null
  tone?: 'neutral' | 'green' | 'red'
}

export function Toast({ msg, tone = 'neutral' }: ToastProps) {
  if (!msg) return null
  return <div className={`tti-toast tti-toast--${tone}`}>{msg}</div>
}
