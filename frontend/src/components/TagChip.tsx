interface TagChipProps {
  tagKey: string
  value: string
  onClick?: (e: React.MouseEvent) => void
  active?: boolean
}

export function TagChip({ tagKey, value, onClick, active = false }: TagChipProps) {
  return (
    <button
      type="button"
      className={`tti-tag${active ? ' tti-tag--active' : ''}${onClick ? ' tti-tag--clickable' : ''}`}
      onClick={onClick}
      tabIndex={onClick ? 0 : -1}
    >
      <span className="tti-tag__k">{tagKey}</span>
      <span className="tti-tag__sep">:</span>
      <span className="tti-tag__v">{value}</span>
    </button>
  )
}
