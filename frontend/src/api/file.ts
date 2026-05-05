import { post, upload } from './client'
import type { FileDownloadTokenVO } from './types'

export function uploadFile(file: File) {
  return upload<Record<string, unknown>>('/files/upload', file)
}

export function getDownloadToken(fileId: string) {
  return post<FileDownloadTokenVO>(`/files/${fileId}/download-token`)
}

export function getPreviewToken(fileId: string) {
  return post<FileDownloadTokenVO>(`/files/${fileId}/preview-token`)
}

export function downloadUrl(token: string) {
  return `/api/v1/files/d/${token}`
}

export function previewUrl(token: string) {
  return `/api/v1/files/p/${token}`
}
