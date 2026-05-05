import { get, post, put, del } from './client'
import type {
  McpDetailVO,
  McpSummaryVO,
  McpCreateRequest,
  McpUpdateRequest,
  McpListParams,
  PageResult,
} from './types'

export function listMcps(params?: McpListParams) {
  return get<PageResult<McpSummaryVO>>('/mcps', params as Record<string, string | number | boolean | undefined>)
}

export function getMcp(id: string) {
  return get<McpDetailVO>(`/mcps/${id}`)
}

export function createMcp(data: McpCreateRequest) {
  return post<McpDetailVO>('/mcps', data)
}

export function updateMcp(id: string, data: McpUpdateRequest) {
  return put<McpDetailVO>(`/mcps/${id}`, data)
}

export function deleteMcp(id: string, force = false) {
  return del(`/mcps/${id}`, { force })
}

export function toggleMcp(id: string, enabled: boolean) {
  return put<McpDetailVO>(`/mcps/${id}/toggle?enabled=${enabled}`)
}

export function testMcpConnection(id: string) {
  return post<McpDetailVO>(`/mcps/${id}/test`)
}

export function discoverMcpTools(id: string) {
  return post<McpDetailVO>(`/mcps/${id}/discover`)
}

export function exportMcp(id: string) {
  return get<Record<string, unknown>>(`/mcps/${id}/export`)
}
