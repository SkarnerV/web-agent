import { get, post, put, del, upload } from './client'
import type {
  KnowledgeBaseDetailVO,
  KnowledgeBaseSummaryVO,
  KnowledgeBaseCreateRequest,
  KnowledgeBaseUpdateRequest,
  KnowledgeBaseListParams,
  KbDocumentVO,
  KbSearchRequest,
  KbSearchResult,
  PageResult,
} from './types'

export function listKnowledgeBases(params?: KnowledgeBaseListParams) {
  return get<PageResult<KnowledgeBaseSummaryVO>>('/knowledge-bases', params as Record<string, string | number | boolean | undefined>)
}

export function getKnowledgeBase(id: string) {
  return get<KnowledgeBaseDetailVO>(`/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data: KnowledgeBaseCreateRequest) {
  return post<KnowledgeBaseDetailVO>('/knowledge-bases', data)
}

export function updateKnowledgeBase(id: string, data: KnowledgeBaseUpdateRequest) {
  return put<KnowledgeBaseDetailVO>(`/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id: string, force = false) {
  return del(`/knowledge-bases/${id}`, { force })
}

export function exportKnowledgeBase(id: string) {
  return get<Record<string, unknown>>(`/knowledge-bases/${id}/export`)
}

export function uploadDocument(kbId: string, file: File) {
  return upload<Record<string, unknown>>(`/knowledge-bases/${kbId}/documents`, file)
}

export function listDocuments(kbId: string, page?: number, pageSize?: number) {
  return get<PageResult<KbDocumentVO>>(`/knowledge-bases/${kbId}/documents`, {
    page: page ?? 1,
    page_size: pageSize ?? 20,
  })
}

export function deleteDocument(kbId: string, docId: string) {
  return del(`/knowledge-bases/${kbId}/documents/${docId}`)
}

export function reindexDocument(kbId: string, docId: string) {
  return post<void>(`/knowledge-bases/${kbId}/documents/${docId}/reindex`)
}

export function searchKnowledgeBase(kbId: string, data: KbSearchRequest) {
  return post<KbSearchResult[]>(`/knowledge-bases/${kbId}/search`, data)
}
