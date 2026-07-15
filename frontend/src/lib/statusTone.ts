// Tone (color family) for each process/deadline status. Lives outside the
// StatusPill component file so component files only export components
// (react-refresh requirement).
export const STATUS_TONE: Record<string, string> = {
  ACTIVE: 'blue',
  COMPLETED: 'green',
  FAILED: 'red',
  ON_TRACK: 'green',
  MISSED: 'red',
  COMPLETED_ON_TIME: 'green',
  COMPLETED_LATE: 'amber',
}
