type IconName =
  | 'search' | 'plus' | 'x' | 'chevron_right' | 'chevron_down' | 'arrow_right'
  | 'activity' | 'check' | 'alert' | 'fail' | 'clock' | 'filter' | 'sort'
  | 'list' | 'grid' | 'book' | 'settings' | 'bell' | 'play' | 'refresh'
  | 'copy' | 'chart' | 'tag' | 'layers' | 'folder' | 'trash'

const PATHS: Record<IconName, React.ReactNode> = {
  search: <><circle cx="11" cy="11" r="6.5" /><path d="M16 16l4 4" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  x: <path d="M6 6l12 12M18 6L6 18" />,
  chevron_right: <path d="M9 6l6 6-6 6" />,
  chevron_down: <path d="M6 9l6 6 6-6" />,
  arrow_right: <path d="M5 12h14M13 6l6 6-6 6" />,
  activity: <path d="M3 12h4l3-9 4 18 3-9h4" />,
  check: <path d="M5 12l4 4 10-10" />,
  alert: <><path d="M12 8v5" /><path d="M12 17h.01" /><circle cx="12" cy="12" r="9.5" /></>,
  fail: <><circle cx="12" cy="12" r="9.5" /><path d="M9 9l6 6M15 9l-6 6" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  filter: <path d="M4 5h16M7 12h10M10 19h4" />,
  sort: <path d="M7 4v16M3 8l4-4 4 4M17 20V4M13 16l4 4 4-4" />,
  list: <path d="M4 6h16M4 12h16M4 18h16" />,
  grid: <><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /></>,
  book: <><path d="M4 4h12a4 4 0 0 1 4 4v12H8a4 4 0 0 1-4-4V4z" /><path d="M4 16a4 4 0 0 1 4-4h12" /></>,
  settings: <><circle cx="12" cy="12" r="3" /><path d="M19 12a7 7 0 0 0-.1-1.2l2-1.5-2-3.4-2.3.9a7 7 0 0 0-2-1.2L14 3h-4l-.6 2.6a7 7 0 0 0-2 1.2l-2.3-.9-2 3.4 2 1.5A7 7 0 0 0 5 12a7 7 0 0 0 .1 1.2l-2 1.5 2 3.4 2.3-.9a7 7 0 0 0 2 1.2L10 21h4l.6-2.6a7 7 0 0 0 2-1.2l2.3.9 2-3.4-2-1.5A7 7 0 0 0 19 12z" /></>,
  bell: <><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9z" /><path d="M10 21a2 2 0 0 0 4 0" /></>,
  play: <path d="M6 4l14 8-14 8z" />,
  refresh: <><path d="M3 12a9 9 0 0 1 15-6.7L21 8" /><path d="M21 3v5h-5" /><path d="M21 12a9 9 0 0 1-15 6.7L3 16" /><path d="M3 21v-5h5" /></>,
  copy: <><rect x="8" y="8" width="12" height="12" rx="2" /><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3" /></>,
  chart: <><path d="M3 3v18h18" /><path d="M7 14l4-4 3 3 5-6" /></>,
  tag: <><path d="M20.6 13.4 12.4 21.6a2 2 0 0 1-2.8 0L2.4 14.4a2 2 0 0 1 0-2.8L11.6 2.4a2 2 0 0 1 1.4-.6h6.6A2 2 0 0 1 21.6 4v6.6a2 2 0 0 1-1 1.7z" /><circle cx="16" cy="8" r="1.5" /></>,
  layers: <><path d="M12 3l9 5-9 5-9-5z" /><path d="M3 13l9 5 9-5" /></>,
  folder: <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />,
  trash: <><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6" /><path d="M10 11v6M14 11v6" /></>,
}

interface IconProps {
  name: IconName
  size?: number
}

export function Icon({ name, size = 16 }: IconProps) {
  const content = PATHS[name]
  if (!content) return null
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {content}
    </svg>
  )
}
