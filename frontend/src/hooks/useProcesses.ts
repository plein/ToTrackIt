import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import {
  listProcesses,
  createProcess,
  completeProcess,
  deleteProcess,
  getTagImpact,
  getSummary,
  getNameRollups,
} from '@/api/processes'
import type { ProcessFilter, NewProcessRequest, CompleteProcessRequest } from '@/types'

export const PAGE_SIZE = 50

// One bounded request per view; filtering/sorting/pagination happen server-side.
export function useProcessList(filter: ProcessFilter = {}) {
  return useQuery({
    queryKey: ['processes', filter],
    queryFn: () => listProcesses(filter),
    placeholderData: keepPreviousData,
    refetchInterval: 10_000,
  })
}

export function useSummary() {
  return useQuery({
    queryKey: ['summary'],
    queryFn: getSummary,
    refetchInterval: 10_000,
  })
}

export function useNameRollups(limit = 20, offset = 0) {
  return useQuery({
    queryKey: ['name-rollups', limit, offset],
    queryFn: () => getNameRollups(limit, offset),
    placeholderData: keepPreviousData,
    refetchInterval: 10_000,
  })
}

export function useTagImpact(name?: string, windowHours = 24) {
  return useQuery({
    queryKey: ['tag-impact', name ?? null, windowHours],
    queryFn: () => getTagImpact(name, windowHours),
    refetchInterval: 30_000,
  })
}

function invalidateProcessData(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['processes'] })
  qc.invalidateQueries({ queryKey: ['summary'] })
  qc.invalidateQueries({ queryKey: ['name-rollups'] })
}

export function useCreateProcess() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ name, body }: { name: string; body: NewProcessRequest }) =>
      createProcess(name, body),
    onSuccess: () => invalidateProcessData(qc),
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
    onSuccess: () => invalidateProcessData(qc),
  })
}

export function useDeleteProcess() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ name, id }: { name: string; id: string }) => deleteProcess(name, id),
    onSuccess: () => invalidateProcessData(qc),
  })
}
