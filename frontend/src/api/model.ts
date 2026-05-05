import { get, post, put, del } from './client'
import type {
  BuiltinModelVO,
  CustomModelVO,
  CustomModelCreateRequest,
  CustomModelUpdateRequest,
  ModelInfo,
} from './types'

export function listBuiltinModels() {
  return get<BuiltinModelVO[]>('/models/builtin')
}

export function listAllModels() {
  return get<ModelInfo[]>('/models/all')
}

export function listCustomModels() {
  return get<CustomModelVO[]>('/models')
}

export function createCustomModel(data: CustomModelCreateRequest) {
  return post<CustomModelVO>('/models', data)
}

export function updateCustomModel(id: string, data: CustomModelUpdateRequest) {
  return put<CustomModelVO>(`/models/${id}`, data)
}

export function deleteCustomModel(id: string) {
  return del(`/models/${id}`)
}
