// ── Backend response wrappers ──

export interface ApiResponse<T> {
  success: boolean
  data: T
  requestId?: string
  timestamp?: string
}

export interface PageResult<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
}

// ── Agent types ──

export type AgentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type AgentVisibility = 'PRIVATE' | 'WORKSPACE'

export interface AgentSummaryVO {
  id: string
  name: string
  description?: string
  avatar?: string
  status: AgentStatus
  visibility: AgentVisibility
  currentVersion?: string
  hasUnpublishedChanges: boolean
  ownerId: string
  createdAt: string
  updatedAt: string
}

export interface ToolBindingVO {
  id: string
  sourceType: string
  sourceId: string
  toolName: string
  toolSchemaSnapshot?: string
  enabled: boolean
  sortOrder: number
}

export interface AgentDetailVO extends AgentSummaryVO {
  systemPrompt?: string
  maxSteps: number
  modelId?: string
  version: number
  toolBindings: ToolBindingVO[]
  skillIds: string[]
  knowledgeBaseIds: string[]
}

export interface ToolBindingRequest {
  sourceType: string
  sourceId: string
  toolName: string
  enabled?: boolean
}

export interface AgentCreateRequest {
  name: string
  description?: string
  avatar?: string
  systemPrompt?: string
  maxSteps?: number
  modelId?: string
  toolBindings?: ToolBindingRequest[]
  skillIds?: string[]
  knowledgeBaseIds?: string[]
  collaboratorAgentIds?: string[]
}

export interface AgentUpdateRequest extends Partial<AgentCreateRequest> {
  version?: number
}

export interface AgentImportResult {
  agent: AgentDetailVO
  unresolvedRefs: Record<string, unknown>[]
}

export interface AssetVersionVO {
  id: string
  assetType: string
  assetId: string
  version: string
  releaseNotes?: string
  publishedBy: string
  publishedAt: string
}

export interface AgentListParams {
  status?: AgentStatus
  search?: string
  page?: number
  page_size?: number
  sort_by?: string
  sort_order?: 'asc' | 'desc'
}

// ── Chat types ──

export interface CreateSessionRequest {
  agentId: string
}

export interface ChatSessionVO {
  id: string
  userId: string
  currentAgentId: string
  title?: string
  createdAt: string
  updatedAt: string
}

export type MessageRole = 'user' | 'assistant' | 'system' | 'tool'
export type MessageStatus = 'SENDING' | 'SENT' | 'STREAMING' | 'COMPLETED' | 'FAILED' | 'INTERRUPTED'

export interface ChatMessageVO {
  id: string
  sessionId: string
  role: MessageRole
  content?: string
  status: MessageStatus
  toolCalls?: string
  toolResults?: string
  attachments?: string
  agentId?: string
  modelId?: string
  stepCount?: number
  usage?: string
  createdAt: string
}

export interface ChatSessionDetailVO extends ChatSessionVO {
  messages: ChatMessageVO[]
}

export interface SendMessageRequest {
  content: string
  attachments?: string[]
  idempotencyKey?: string
}

export interface SwitchAgentRequest {
  agentId: string
}

export interface ContinueRequest {
  sessionStateId: string
}

// ── SSE event types ──

export interface SseEventBase {
  request_id: string
  message_id: string
  timestamp: string
}

export interface SseMessageStart extends SseEventBase {
  agent_id: string
  model: string
}

export interface SseToken extends SseEventBase {
  delta: string
  seq: number
}

export interface SseToolCallStart extends SseEventBase {
  tool_call_id: string
  tool_name: string
  arguments: string
  step_number: number
}

export interface SseToolCallEnd extends SseEventBase {
  tool_call_id: string
  status: string
  result_summary: string
  duration_ms: number
}

export interface SseCitation extends SseEventBase {
  sources: Record<string, unknown>[]
}

export interface SseStepLimit {
  current_step: number
  max_steps: number
  session_state_id: string
}

export interface SseMessageEnd extends SseEventBase {
  finish_reason: string
  usage: Record<string, unknown>
  total_steps: number
}

export interface SseError {
  code: string
  message: string
  recoverable: boolean
  request_id: string
}

export type SseEventType =
  | 'message_start'
  | 'token'
  | 'tool_call_start'
  | 'tool_call_end'
  | 'citation'
  | 'step_limit'
  | 'message_end'
  | 'error'
  | 'heartbeat'

export interface SseEvent<T = unknown> {
  id: string
  type: SseEventType
  data: T
}
