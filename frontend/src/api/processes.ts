import type {
  ProcessResponse,
  PagedResult,
  NewProcessRequest,
  CompleteProcessRequest,
  ProcessFilter,
  ApiError,
} from '@/types'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  const body = await res.json()
  if (!res.ok) throw body as ApiError
  return body as T
}

// Normalize API response to fill in optional fields the backend may omit
function normalize(p: ProcessResponse): ProcessResponse {
  return {
    ...p,
    tags: p.tags ?? [],
    context: p.context ?? {},
    deadline_status: p.deadline_status ?? null,
    completed_at: p.completed_at ?? null,
    deadline: p.deadline ?? null,
    duration: p.duration ?? null,
  }
}

export async function listProcesses(filter: ProcessFilter = {}): Promise<PagedResult<ProcessResponse>> {
  const params = new URLSearchParams()
  Object.entries(filter).forEach(([k, v]) => {
    if (v !== undefined && v !== '') params.set(k, String(v))
  })
  const qs = params.toString()
  const result = await request<PagedResult<ProcessResponse>>(`/processes/${qs ? `?${qs}` : ''}`)
  return { ...result, data: result.data.map(normalize) }
}

export async function getProcess(name: string, id: string): Promise<ProcessResponse> {
  const p = await request<ProcessResponse>(`/processes/${encodeURIComponent(name)}/${encodeURIComponent(id)}`)
  return normalize(p)
}

export async function createProcess(name: string, body: NewProcessRequest): Promise<ProcessResponse> {
  const p = await request<ProcessResponse>(`/processes/${encodeURIComponent(name)}`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
  return normalize(p)
}

export async function completeProcess(
  name: string,
  id: string,
  body: CompleteProcessRequest,
): Promise<ProcessResponse> {
  const p = await request<ProcessResponse>(
    `/processes/${encodeURIComponent(name)}/${encodeURIComponent(id)}/complete`,
    { method: 'PUT', body: JSON.stringify(body) },
  )
  return normalize(p)
}
