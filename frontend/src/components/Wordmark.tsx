export function Wordmark() {
  return (
    <div className="tti-wordmark" aria-label="ToTrackIt">
      <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="10" fill="none" stroke="var(--accent)" strokeWidth="1.6" />
        <path d="M12 6v6l4 2.5" fill="none" stroke="var(--accent)" strokeWidth="1.6" strokeLinecap="round" />
        <circle cx="12" cy="12" r="1.6" fill="var(--accent)" />
      </svg>
      <span className="tti-wordmark__text">
        <b>ToTrack</b>It
      </span>
    </div>
  )
}
