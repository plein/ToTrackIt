import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { listProcesses, createProcess, completeProcess, deleteProcess } from '@/api/processes'
import type { ProcessFilter, ProcessResponse, NewProcessRequest, CompleteProcessRequest } from '@/types'

const PAGE_SIZE = 100

async function fetchAllProcesses(filter: ProcessFilter): Promise<ProcessResponse[]> {
  let offset = 0
  const all: ProcessResponse[] = []
  while (true) {
    const page = await listProcesses({ ...filter, limit: PAGE_SIZE, offset })
    all.push(...page.data)
    if (!page.has_more) break
    offset += PAGE_SIZE
  }
  return all
}

export function useProcessList(filter: ProcessFilter = {}) {
  return useQuery({
    queryKey: ['processes', filter],
    queryFn: () => fetchAllProcesses(filter),
    refetchInterval: 10_000,
  })
}

export function useCreateProcess() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ name, body }: { name: string; body: NewProcessRequest }) =>
      createProcess(name, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['processes'] }),
  })
}

export function useCompleteProcess() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      name,
      id,
      body,
    }: {
      name: string
      id: string
      body: CompleteProcessRequest
    }) => completeProcess(name, id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['processes'] }),
  })
}

export function useDeleteProcess() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ name, id }: { name: string; id: string }) => deleteProcess(name, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['processes'] }),
  })
}
