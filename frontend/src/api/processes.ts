import type {
  ProcessResponse,
  PagedResult,
  NewProcessRequest,
  CompleteProcessRequest,
  ProcessFilter,
  ApiError,
} from '@/types'

function extractMessage(body: unknown): string {
  if (!body || typeof body !== 'object') return 'Unknown error'
  const b = body as Record<string, unknown>
  // Custom ErrorResponse: { error, message, ... }
  if (typeof b.message === 'string' && b.error) return b.message
  // Micronaut HATEOAS default: { _embedded: { errors: [{ message }] }, message }
  const embedded = b._embedded as Record<string, unknown> | undefined
  if (Array.isArray(embedded?.errors)) {
    const first = (embedded!.errors as Record<string, unknown>[])[0]
    if (typeof first?.message === 'string') return first.message
  }
  if (typeof b.message === 'string') return b.message
  return 'Request failed'
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  const body = await res.json()
  if (!res.ok) {
    const err: ApiError = {
      error: (body as Record<string, unknown>)?.error as string ?? 'ERROR',
      message: extractMessage(body),
      timestamp: Math.floor(Date.now() / 1000),
      path,
    }
    throw err
  }
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

export async function deleteProcess(name: string, id: string): Promise<void> {
  const res = await fetch(
    `/processes/${encodeURIComponent(name)}/${encodeURIComponent(id)}`,
    { method: 'DELETE', headers: { 'Content-Type': 'application/json' } },
  )
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw {
      error: 'DELETE_FAILED',
      message: extractMessage(body),
      timestamp: Math.floor(Date.now() / 1000),
      path: `/processes/${name}/${id}`,
    }
  }
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
