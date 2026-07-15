export type ProcessStatus = 'ACTIVE' | 'COMPLETED' | 'FAILED'
export type DeadlineStatus = 'ON_TRACK' | 'MISSED' | 'COMPLETED_ON_TIME' | 'COMPLETED_LATE'

export interface ProcessTag {
  key: string
  value: string
}

export interface ProcessResponse {
  id: string
  name: string
  status: ProcessStatus
  deadline_status: DeadlineStatus | null
  started_at: number
  completed_at: number | null
  deadline: number | null
  tags: ProcessTag[]
  context: Record<string, unknown>
  duration: number | null
}

export interface PagedResult<T> {
  data: T[]
  total: number
  limit: number
  offset: number
  has_more: boolean
}

export interface NewProcessRequest {
  id: string
  deadline?: number
  tags?: ProcessTag[]
  context?: Record<string, unknown>
}

export interface CompleteProcessRequest {
  status: 'COMPLETED' | 'FAILED'
}

export interface ProcessFilter {
  name?: string
  status?: ProcessStatus
  deadline_status?: DeadlineStatus
  deadline_before?: number
  deadline_after?: number
  running_duration_min?: number
  sort_by?: string
  limit?: number
  offset?: number
  tags?: string
}

export interface DurationStats {
  count: number
  avg_seconds: number
  p50_seconds: number
  p90_seconds: number
  p99_seconds: number
}

export interface TagImpactEntry {
  key: string
  value: string
  overdue: number
  on_track: number
  completed_late: number
  completed_on_time: number
  failed: number
  total: number
  problems: number
  duration?: DurationStats | null
}

export interface TagImpactResponse {
  window_hours: number
  generated_at: number
  total_processes: number
  problem_processes: number
  duration?: DurationStats | null
  tags: TagImpactEntry[]
}

export interface ApiError {
  error: string
  message: string
  timestamp: number
  path: string
}
