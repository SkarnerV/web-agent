import type { ApiResponse } from './types'

const BASE_URL = '/api/v1'

class ApiError extends Error {
  status: number
  body: unknown

  constructor(status: number, body: unknown) {
    const msg =
      typeof body === 'object' && body !== null && 'message' in body
        ? String((body as Record<string, unknown>).message)
        : `Request failed with status ${status}`
    super(msg)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

async function request<T>(
  method: string,
  path: string,
  opts?: {
    body?: unknown
    params?: Record<string, string | number | boolean | undefined>
    headers?: Record<string, string>
  },
): Promise<T> {
  const url = new URL(`${BASE_URL}${path}`, window.location.origin)

  if (opts?.params) {
    Object.entries(opts.params).forEach(([k, v]) => {
      if (v !== undefined && v !== '') {
        url.searchParams.set(k, String(v))
      }
    })
  }

  const headers: Record<string, string> = {
    ...(opts?.headers ?? {}),
  }

  let body: BodyInit | undefined
  if (opts?.body !== undefined) {
    if (opts.body instanceof FormData) {
      body = opts.body
    } else {
      headers['Content-Type'] = 'application/json'
      body = JSON.stringify(opts.body)
    }
  }

  const res = await fetch(url.toString(), { method, headers, body })

  if (res.status === 204) {
    return undefined as T
  }

  const json: ApiResponse<T> = await res.json()

  if (!res.ok || !json.success) {
    throw new ApiError(res.status, json)
  }

  return json.data
}

export function get<T>(
  path: string,
  params?: Record<string, string | number | boolean | undefined>,
): Promise<T> {
  return request<T>('GET', path, { params })
}

export function post<T>(
  path: string,
  body?: unknown,
): Promise<T> {
  return request<T>('POST', path, { body })
}

export function put<T>(
  path: string,
  body?: unknown,
): Promise<T> {
  return request<T>('PUT', path, { body })
}

export function del<T = void>(path: string): Promise<T> {
  return request<T>('DELETE', path)
}

export function upload<T>(path: string, file: File): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)
  return request<T>('POST', path, { body: formData })
}

export { ApiError }
export { BASE_URL }
