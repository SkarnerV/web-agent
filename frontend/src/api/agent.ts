import { get, post, put, del, upload } from './client'
import type {
  AgentDetailVO,
  AgentSummaryVO,
  AgentCreateRequest,
  AgentUpdateRequest,
  AgentImportResult,
  AgentListParams,
  AssetVersionVO,
  PageResult,
} from './types'

export function listAgents(params?: AgentListParams) {
  return get<PageResult<AgentSummaryVO>>('/agents', params as Record<string, string | number | boolean | undefined>)
}

export function getAgent(id: string) {
  return get<AgentDetailVO>(`/agents/${id}`)
}

export function createAgent(data: AgentCreateRequest) {
  return post<AgentDetailVO>('/agents', data)
}

export function updateAgent(id: string, data: AgentUpdateRequest) {
  return put<AgentDetailVO>(`/agents/${id}`, data)
}

export function deleteAgent(id: string) {
  return del(`/agents/${id}`)
}

export function duplicateAgent(id: string) {
  return post<AgentDetailVO>(`/agents/${id}/duplicate`)
}

export function exportAgent(id: string) {
  return get<Record<string, unknown>>(`/agents/${id}/export`)
}

export function importAgent(file: File) {
  return upload<AgentImportResult>('/agents/import', file)
}

export function listAgentVersions(id: string) {
  return get<AssetVersionVO[]>(`/agents/${id}/versions`)
}

export function rollbackAgent(id: string, versionId: string) {
  return post<AgentDetailVO>(`/agents/${id}/versions/${versionId}/rollback`)
}
