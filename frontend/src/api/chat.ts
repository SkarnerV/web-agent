import { get, post, del } from './client'
import type {
  ChatSessionVO,
  ChatSessionDetailVO,
  CreateSessionRequest,
  SendMessageRequest,
  SwitchAgentRequest,
  PageResult,
  SseEvent,
} from './types'

export function listSessions(page?: number, pageSize?: number) {
  return get<PageResult<ChatSessionVO>>('/chat/sessions', {
    page: page ?? 1,
    page_size: pageSize ?? 50,
  })
}

export function getSession(id: string) {
  return get<ChatSessionDetailVO>(`/chat/sessions/${id}`)
}

export function createSession(data: CreateSessionRequest) {
  return post<ChatSessionVO>('/chat/sessions', data)
}

export function clearMessages(id: string) {
  return del(`/chat/sessions/${id}/messages`)
}

export function switchAgent(sessionId: string, data: SwitchAgentRequest) {
  return post<void>(`/chat/sessions/${sessionId}/switch-agent`, data)
}

/**
 * Send a message via SSE. Returns an abort controller and an async generator of SSE events.
 */
export function sendMessage(
  sessionId: string,
  data: SendMessageRequest,
  lastEventId?: string,
): { abort: () => void; stream: AsyncGenerator<SseEvent> } {
  return sseStream(`/chat/sessions/${sessionId}/messages`, {
    method: 'POST',
    body: JSON.stringify(data),
    lastEventId,
  })
}

export function regenerateMessage(
  sessionId: string,
  messageId: string,
): { abort: () => void; stream: AsyncGenerator<SseEvent> } {
  return sseStream(`/chat/sessions/${sessionId}/messages/${messageId}/regenerate`, {
    method: 'POST',
  })
}

export function continueSession(
  sessionId: string,
  sessionStateId: string,
): { abort: () => void; stream: AsyncGenerator<SseEvent> } {
  return sseStream(`/chat/sessions/${sessionId}/continue`, {
    method: 'POST',
    body: JSON.stringify({ sessionStateId }),
  })
}

// ── SSE stream parser ──

interface SseStreamOptions {
  method: 'POST'
  body?: string
  lastEventId?: string
}

function sseStream(
  path: string,
  opts: SseStreamOptions,
): { abort: () => void; stream: AsyncGenerator<SseEvent> } {
  const controller = new AbortController()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  if (opts.lastEventId) {
    headers['Last-Event-ID'] = opts.lastEventId
  }

  const url = `/api/v1${path}`

  const stream = parseSseStream(url, {
    method: opts.method,
    headers,
    body: opts.body,
    signal: controller.signal,
  })

  return { abort: () => controller.abort(), stream }
}

async function* parseSseStream(
  url: string,
  init: RequestInit,
): AsyncGenerator<SseEvent> {
  const res = await fetch(url, init)

  if (!res.ok) {
    const body = await res.text()
    let message = `SSE request failed: ${res.status}`
    try {
      const json = JSON.parse(body)
      if (json.message) message = json.message
    } catch { /* ignore */ }
    throw new Error(message)
  }

  const reader = res.body?.getReader()
  if (!reader) throw new Error('Response body is not readable')

  const decoder = new TextDecoder()
  let buffer = ''
  let currentId = ''
  let currentEvent = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      // Keep the last partial line in buffer
      buffer = lines.pop() ?? ''

      for (const line of lines) {
        if (line.startsWith('id:')) {
          currentId = line.slice(3).trim()
        } else if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const jsonStr = line.slice(5).trim()
          if (!jsonStr) continue
          try {
            const data = JSON.parse(jsonStr)
            yield {
              id: currentId,
              type: (currentEvent || 'message') as SseEvent['type'],
              data,
            }
          } catch {
            // skip unparseable events
          }
          currentId = ''
          currentEvent = ''
        }
        // Empty lines are SSE comment/separator — reset
      }
    }
  } finally {
    reader.releaseLock()
  }
}
