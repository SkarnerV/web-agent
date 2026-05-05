import { get, post, put, del } from './client'
import type {
  SkillDetailVO,
  SkillSummaryVO,
  SkillCreateRequest,
  SkillUpdateRequest,
  SkillListParams,
  PageResult,
} from './types'

export function listSkills(params?: SkillListParams) {
  return get<PageResult<SkillSummaryVO>>('/skills', params as Record<string, string | number | boolean | undefined>)
}

export function getSkill(id: string) {
  return get<SkillDetailVO>(`/skills/${id}`)
}

export function createSkill(data: SkillCreateRequest) {
  return post<SkillDetailVO>('/skills', data)
}

export function updateSkill(id: string, data: SkillUpdateRequest) {
  return put<SkillDetailVO>(`/skills/${id}`, data)
}

export function deleteSkill(id: string, force = false) {
  return del(`/skills/${id}`, { force })
}

export function exportSkill(id: string) {
  return get<Record<string, unknown>>(`/skills/${id}/export`)
}
