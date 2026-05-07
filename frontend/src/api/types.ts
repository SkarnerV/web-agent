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

export type AgentStatus = 'draft' | 'published' | 'archived'
export type AgentVisibility = 'private' | 'public' | 'group_edit' | 'group_read'

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
  sourceId?: string
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

// ── Skill types ──

export interface SkillCreateRequest {
  name: string
  description?: string
  triggerConditions?: string
  format?: string
  content?: string
}

export interface SkillUpdateRequest extends SkillCreateRequest {
  version?: number
}

export interface SkillDetailVO {
  id: string
  ownerId: string
  name: string
  description?: string
  triggerConditions?: string
  format?: string
  content?: string
  status: string
  visibility: string
  currentVersion?: string
  hasUnpublishedChanges: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface SkillSummaryVO {
  id: string
  name: string
  description?: string
  status: string
  visibility: string
  ownerId: string
  createdAt: string
}

export interface SkillListParams {
  status?: string
  search?: string
  page?: number
  page_size?: number
  sort_by?: string
  sort_order?: 'asc' | 'desc'
}

// ── MCP types ──

export interface McpCreateRequest {
  name: string
  description?: string
  url?: string
  protocol?: string
  authHeaders?: string
  jsonConfig?: string
}

export interface McpUpdateRequest {
  name?: string
  url?: string
  protocol?: string
  authHeaders?: string
  jsonConfig?: string
  version?: number
}

export interface McpDetailVO {
  id: string
  ownerId: string
  name: string
  description?: string
  url?: string
  protocol?: string
  authHeadersMasked?: string
  jsonConfig?: string
  enabled: boolean
  connectionStatus?: string
  lastError?: string
  toolsDiscovered?: string
  status: string
  visibility: string
  currentVersion?: string
  hasUnpublishedChanges: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface McpSummaryVO {
  id: string
  name: string
  url?: string
  protocol?: string
  enabled: boolean
  connectionStatus?: string
  toolsDiscoveredCount?: number
  status: string
  visibility: string
  ownerId: string
  createdAt: string
}

export interface McpListParams {
  search?: string
  page?: number
  page_size?: number
  sort_by?: string
  sort_order?: 'asc' | 'desc'
}

// ── Knowledge Base types ──

export interface KnowledgeBaseCreateRequest {
  name: string
  description?: string
  indexConfig?: string
}

export interface KnowledgeBaseUpdateRequest {
  name?: string
  description?: string
  indexConfig?: string
  version?: number
}

export interface KnowledgeBaseDetailVO {
  id: string
  ownerId: string
  name: string
  description?: string
  indexConfig?: string
  visibility: string
  docCount: number
  totalSizeBytes: number
  createdAt: string
  updatedAt: string
  version: number
}

export interface KnowledgeBaseSummaryVO {
  id: string
  name: string
  description?: string
  visibility: string
  docCount: number
  totalSizeBytes: number
  ownerId: string
  createdAt: string
}

export interface KnowledgeBaseListParams {
  search?: string
  page?: number
  page_size?: number
  sort_by?: string
  sort_order?: 'asc' | 'desc'
}

export interface KbDocumentVO {
  id: string
  knowledgeBaseId: string
  fileId: string
  filename: string
  fileSize: number
  mimeType?: string
  scanStatus?: string
  indexStatus?: string
  indexError?: string
  chunkCount?: number
  createdAt: string
  updatedAt: string
}

export interface KbSearchRequest {
  query: string
  topK?: number
}

export interface KbSearchResult {
  content: string
  score: number
  documentName: string
  chunkIndex: number
}

// ── File types ──

export interface FileVO {
  id: string
  filename: string
  fileSize: number
  mimeType?: string
  scanStatus?: string
  status?: string
  expiresAt?: string
  createdAt: string
}

export interface FileDownloadTokenVO {
  downloadUrl: string
  expiresAt: string
}

// ── Market types ──

export type MarketAssetType = 'AGENT' | 'SKILL' | 'MCP'

export interface PublishRequest {
  assetType: MarketAssetType
  assetId: string
  visibility?: string
  version?: string
  releaseNotes?: string
}

export interface VisibilityUpdateRequest {
  visibility: string
}

export interface ReviewCreateRequest {
  rating: number
  comment?: string
}

export interface MarketItemVO {
  id: string
  assetType: MarketAssetType
  assetId: string
  currentVersionId?: string
  authorId: string
  authorName?: string
  status: string
  visibility: string
  category?: string
  tags?: string
  useCount: number
  favoriteCount: number
  avgRating?: string
  reviewCount: number
  createdAt: string
  updatedAt: string
}

export interface MarketItemDetailVO extends MarketItemVO {
  configSnapshot?: string
}

export interface ReviewVO {
  id: string
  marketItemId: string
  userId: string
  userName?: string
  rating: number
  comment?: string
  createdAt: string
}

export interface MarketListParams {
  type?: string
  category?: string
  search?: string
  tags?: string
  page?: number
  page_size?: number
  sort_by?: string
  sort_order?: 'asc' | 'desc'
}

// ── Model types ──

export interface BuiltinModelVO {
  id: string
  name: string
  provider?: string
  description?: string
  isDefault: boolean
  enabled: boolean
  sortOrder?: number
}

export interface CustomModelCreateRequest {
  name: string
  apiUrl: string
  apiKey: string
}

export interface CustomModelUpdateRequest {
  name?: string
  apiUrl?: string
  apiKey?: string
}

export interface CustomModelVO {
  id: string
  name: string
  apiUrl?: string
  apiKeyMasked?: string
  connectionStatus?: string
  lastError?: string
  agentCount: number
  createdAt: string
  updatedAt: string
}

export interface ModelAffectedAgents {
  count: number
  agents: string[]
}

export type ModelSource = 'BUILTIN' | 'CUSTOM'

export interface ModelInfo {
  id: string
  name: string
  provider?: string
  source: ModelSource
  description?: string
  isDefault: boolean
  enabled: boolean
  sortOrder?: number
  apiUrl?: string
  apiKeyMasked?: string
  connectionStatus?: string
  lastError?: string
  createdAt?: string
  updatedAt?: string
  config?: Record<string, unknown>
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

export type TodoStatus = 'pending' | 'in_progress' | 'completed' | 'blocked'

export interface TodoItem {
  id: string
  title: string
  status: TodoStatus
  detail?: string
}

export interface TodoState {
  title?: string
  items: TodoItem[]
}

export interface QuestionOption {
  id: string
  label: string
  description?: string
}

export interface QuestionCardState {
  toolCallId: string
  sessionStateId?: string
  questionId: string
  question: string
  options: QuestionOption[]
  allowFreeText: boolean
  multiSelect: boolean
  status: 'pending' | 'answering' | 'answered'
  selectedOptionIds?: string[]
  answerText?: string
}

export interface QuestionAnswerRequest {
  questionId: string
  selectedOptionIds?: string[]
  answerText?: string
}

export interface SseTodoUpdated extends SseEventBase {
  tool_call_id: string
  title?: string
  items: TodoItem[]
}

export interface SseQuestion extends SseEventBase {
  tool_call_id: string
  session_state_id: string
  question_id: string
  question: string
  options: QuestionOption[]
  allow_free_text: boolean
  multi_select: boolean
}

export interface SseRunStatus extends SseEventBase {
  status: string
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
  | 'todo_updated'
  | 'question'
  | 'run_status'
  | 'message_end'
  | 'error'
  | 'heartbeat'

export interface SseEvent<T = unknown> {
  id: string
  type: SseEventType
  data: T
}
