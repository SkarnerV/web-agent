import React, { useEffect, useState, useRef, useCallback } from 'react'
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom'
import {
  Plus,
  Send,
  Bot,
  BookOpen,
  Settings2,
  Sparkles,
  Loader2,
  Wrench,
  Search,
  ChevronDown,
  ChevronRight,
  Paperclip,
  PanelRight,
  MoreHorizontal,
  Copy,
  RefreshCw,
  CheckCircle,
  XCircle,
  FileText,
  HelpCircle,
  ListTodo,
  Circle,
  Clock,
} from 'lucide-react'
import { Sidebar } from '../components/layout/Sidebar'
import { ApiError } from '../api/client'
import {
  listSessions,
  getSession,
  createSession,
  deleteSession,
  sendMessage,
  switchAgent,
  regenerateMessage,
  answerQuestion,
} from '../api/chat'
import { listAgents } from '../api/agent'
import type {
  ChatSessionVO,
  ChatMessageVO,
  AgentSummaryVO,
  SseToken,
  SseMessageStart,
  SseToolCallStart,
  SseToolCallEnd,
  SseStepLimit,
  SseError,
  SseCitation,
  SseEvent,
  SseTodoUpdated,
  SseQuestion,
  TodoState,
  TodoStatus,
  QuestionCardState,
} from '../api/types'

// ── Helpers ──

const groupSessions = (sessions: ChatSessionVO[]) => {
  const now = Date.now()
  const groups: { label: string; items: ChatSessionVO[] }[] = [
    { label: '今天', items: [] },
    { label: '昨天', items: [] },
    { label: '更早', items: [] },
  ]
  for (const s of sessions) {
    const diff = now - new Date(s.updatedAt).getTime()
    if (diff < 86400000) {
      groups[0].items.push(s)
    } else if (diff < 172800000) {
      groups[1].items.push(s)
    } else {
      groups[2].items.push(s)
    }
  }
  return groups.filter((g) => g.items.length > 0)
}

const formatTime = (iso: string) => {
  const d = new Date(iso)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── Sub-components ──

const SessionItem: React.FC<{
  id: string
  title: string
  agent: string
  time: string
  lastMessage?: string
  active?: boolean
  isDeleting?: boolean
  onClick: () => void
  onDelete: () => void
}> = ({ title, agent, time, lastMessage, active, isDeleting, onClick, onDelete }) => (
  <div
    role="button"
    tabIndex={0}
    onClick={onClick}
    onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault()
        onClick()
      }
    }}
    className={`w-full min-w-0 p-2.5 rounded-lg text-left transition-colors group relative cursor-pointer ${
      active ? 'bg-brand-50' : 'hover:bg-gray-50'
    }`}
  >
    <div className="min-w-0 flex flex-col gap-1">
      <span className="truncate pr-5 text-sm font-medium text-text-primary">{title || '新对话'}</span>
      {lastMessage && (
        <span className="block truncate pr-2 text-xs text-text-tertiary">{lastMessage}</span>
      )}
      <div className="flex min-w-0 items-center gap-2">
        <span className="min-w-0 flex-1 truncate text-xs text-text-tertiary">{agent}</span>
        <span className="shrink-0 text-xs text-text-tertiary">{formatTime(time)}</span>
      </div>
    </div>
    <button
      onClick={(e) => { e.stopPropagation(); if (!isDeleting) onDelete() }}
      disabled={isDeleting}
      aria-label="删除对话"
      className="absolute top-2 right-2 w-5 h-5 rounded flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 opacity-0 group-hover:opacity-100 transition-all disabled:cursor-not-allowed disabled:opacity-100"
    >
      {isDeleting ? <Loader2 className="w-3 h-3 animate-spin" /> : <span className="text-[10px]">✕</span>}
    </button>
  </div>
)

const ToolCallCard: React.FC<{ tc: ToolCallDetail }> = ({ tc }) => {
  const [expanded, setExpanded] = React.useState(false)
  const StatusIcon = tc.status === 'running'
    ? Loader2
    : tc.status === 'success'
    ? CheckCircle
    : tc.status === 'requires_action'
    ? HelpCircle
    : XCircle
  const statusColor = tc.status === 'running'
    ? 'text-brand-500'
    : tc.status === 'success'
    ? 'text-green-500'
    : tc.status === 'requires_action'
    ? 'text-amber-500'
    : 'text-red-500'

  return (
    <div className="border border-border-subtle rounded-lg bg-white w-full">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center gap-2 px-3 py-2 text-left"
      >
        {expanded ? <ChevronDown className="w-3.5 h-3.5 text-text-tertiary shrink-0" /> : <ChevronRight className="w-3.5 h-3.5 text-text-tertiary shrink-0" />}
        <StatusIcon className={`w-3.5 h-3.5 shrink-0 ${statusColor} ${tc.status === 'running' ? 'animate-spin' : ''}`} />
        <span className="text-xs font-medium text-text-primary truncate">{tc.name}</span>
      </button>
      {expanded && (
        <div className="px-3 pb-3 flex flex-col gap-2 border-t border-border-subtle pt-2">
          {tc.args && (
            <div>
              <span className="text-[10px] font-semibold text-text-tertiary uppercase">Arguments</span>
              <pre className="mt-1 text-[11px] text-text-secondary bg-gray-50 rounded p-2 overflow-x-auto max-h-40 whitespace-pre-wrap break-all">{(() => {
                try { return JSON.stringify(JSON.parse(tc.args), null, 2) } catch { return tc.args }
              })()}</pre>
            </div>
          )}
          {tc.result && (
            <div>
              <span className="text-[10px] font-semibold text-text-tertiary uppercase">Result</span>
              <pre className="mt-1 text-[11px] text-text-secondary bg-gray-50 rounded p-2 overflow-x-auto max-h-40 whitespace-pre-wrap break-all">{tc.result}</pre>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const todoStatusMeta: Record<TodoStatus, { label: string; dot: string; text: string }> = {
  pending: { label: '待处理', dot: 'bg-gray-300', text: 'text-text-tertiary' },
  in_progress: { label: '进行中', dot: 'bg-brand-500', text: 'text-brand-500' },
  completed: { label: '已完成', dot: 'bg-green-500', text: 'text-green-600' },
  blocked: { label: '阻塞', dot: 'bg-red-500', text: 'text-red-500' },
}

const TodoPanel: React.FC<{ todo: TodoState }> = ({ todo }) => {
  const total = todo.items.length
  const done = todo.items.filter((item) => item.status === 'completed').length

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <ListTodo className="w-4 h-4 text-brand-500" />
        <span className="text-sm font-semibold text-text-primary">Agent Todo</span>
        {total > 0 && (
          <span className="ml-auto text-[11px] text-text-tertiary">{done}/{total}</span>
        )}
      </div>
      <div className="rounded-xl border border-border-subtle bg-gray-50 p-3 flex flex-col gap-2">
        <div className="flex items-center justify-between gap-3">
          <span className="text-xs font-semibold text-text-primary truncate">{todo.title || '任务清单'}</span>
          {total > 0 && (
            <span className="text-[11px] text-text-tertiary shrink-0">
              {Math.round((done / total) * 100)}%
            </span>
          )}
        </div>
        <div className="flex flex-col gap-2">
          {todo.items.map((item) => {
            const meta = todoStatusMeta[item.status] ?? todoStatusMeta.pending
            return (
              <div key={item.id} className="rounded-lg bg-white border border-border-subtle p-2.5">
                <div className="flex items-start gap-2">
                  {item.status === 'completed' ? (
                    <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0 mt-0.5" />
                  ) : item.status === 'in_progress' ? (
                    <Clock className="w-3.5 h-3.5 text-brand-500 shrink-0 mt-0.5" />
                  ) : (
                    <Circle className={`w-3.5 h-3.5 shrink-0 mt-0.5 ${meta.text}`} />
                  )}
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-medium text-text-primary truncate">{item.title}</span>
                      <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${meta.dot}`} />
                    </div>
                    {item.detail && (
                      <p className="mt-1 text-[11px] text-text-tertiary line-clamp-2">{item.detail}</p>
                    )}
                    <span className={`mt-1 inline-block text-[10px] ${meta.text}`}>{meta.label}</span>
                  </div>
                </div>
              </div>
            )
          })}
          {todo.items.length === 0 && (
            <p className="text-xs text-text-tertiary">Agent 尚未创建任务。</p>
          )}
        </div>
      </div>
    </div>
  )
}

const QuestionCard: React.FC<{
  question: QuestionCardState
  disabled?: boolean
  onAnswer: (question: QuestionCardState, selectedOptionIds?: string[], answerText?: string) => void
}> = ({ question, disabled, onAnswer }) => {
  const [selected, setSelected] = React.useState<string[]>(question.selectedOptionIds ?? [])
  const [freeText, setFreeText] = React.useState(question.answerText ?? '')
  const locked = disabled || question.status === 'answered' || question.status === 'answering' || !question.sessionStateId

  const toggleOption = (id: string) => {
    if (locked) return
    if (!question.multiSelect) {
      setSelected([id])
      onAnswer(question, [id], undefined)
      return
    }
    setSelected((prev) => prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id])
  }

  const submit = () => {
    const text = freeText.trim()
    onAnswer(question, selected, text || undefined)
  }

  return (
    <div className="w-full rounded-xl border border-amber-200 bg-amber-50/70 p-3 flex flex-col gap-3">
      <div className="flex items-start gap-2">
        <HelpCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-text-primary">{question.question}</p>
          <p className="mt-1 text-[11px] text-text-tertiary">
            {question.status === 'answered' ? '已回答，Agent 正在继续执行。' : 'Agent 需要你选择后继续执行。'}
          </p>
        </div>
      </div>
      <div className="flex flex-col gap-2">
        {question.options.map((option) => {
          const isSelected = selected.includes(option.id)
          return (
            <button
              key={option.id}
              onClick={() => toggleOption(option.id)}
              disabled={locked}
              className={`w-full text-left rounded-lg border px-3 py-2 transition-colors ${
                isSelected
                  ? 'border-amber-500 bg-white text-text-primary'
                  : 'border-amber-100 bg-white/70 text-text-secondary hover:border-amber-300'
              } disabled:cursor-not-allowed disabled:opacity-75`}
            >
              <span className="block text-xs font-semibold">{option.label}</span>
              {option.description && (
                <span className="mt-0.5 block text-[11px] text-text-tertiary">{option.description}</span>
              )}
            </button>
          )
        })}
      </div>
      {question.allowFreeText && question.status !== 'answered' && (
        <textarea
          value={freeText}
          onChange={(e) => setFreeText(e.target.value)}
          disabled={locked}
          rows={2}
          placeholder="也可以补充说明..."
          className="w-full rounded-lg border border-amber-100 bg-white px-3 py-2 text-xs outline-none focus:border-amber-400 disabled:cursor-not-allowed"
        />
      )}
      {(question.multiSelect || question.allowFreeText) && question.status !== 'answered' && (
        <button
          onClick={submit}
          disabled={locked || (selected.length === 0 && !freeText.trim())}
          className="self-end rounded-lg bg-amber-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-amber-600 disabled:bg-gray-200 disabled:text-gray-400"
        >
          {question.status === 'answering' ? '提交中...' : '提交回答'}
        </button>
      )}
    </div>
  )
}

const MessageBubble: React.FC<{
  role: 'user' | 'assistant' | 'system' | 'tool'
  content?: string
  time: string
  avatar: string
  agentName?: string
  toolCallDetails?: ToolCallDetail[]
  questions?: QuestionCardState[]
  status?: string
  isLastAssistant?: boolean
  onRegenerate?: () => void
  onAnswerQuestion?: (question: QuestionCardState, selectedOptionIds?: string[], answerText?: string) => void
}> = ({ role, content, time, avatar, agentName, toolCallDetails, questions, status, isLastAssistant, onRegenerate, onAnswerQuestion }) => {
  const [copied, setCopied] = React.useState(false)

  const handleCopy = () => {
    if (content) {
      navigator.clipboard.writeText(content)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    }
  }
  if (role === 'tool') {
    return (
      <div className="flex gap-3 justify-center">
        <div className="max-w-[70%] px-3 py-2 bg-gray-50 border border-border-subtle rounded-lg text-xs text-text-tertiary">
          <Wrench className="w-3 h-3 inline mr-1" />
          {content || '调用工具...'}
          {status === 'STREAMING' && (
            <span className="ml-1 inline-block w-2 h-2 bg-brand-500 rounded-full animate-pulse" />
          )}
        </div>
      </div>
    )
  }

  const showActions = role === 'assistant' && (status === 'COMPLETED' || status === 'FAILED') && content

  return (
    <div className={`group/msg flex gap-3 ${role === 'user' ? 'justify-end' : 'justify-start'}`}>
      {role === 'assistant' && (
        <div className="w-8 h-8 rounded-full bg-brand-500 flex items-center justify-center shrink-0">
          <span className="text-sm font-semibold text-white">{avatar}</span>
        </div>
      )}
      <div className={`max-w-[70%] flex flex-col gap-1.5 ${role === 'user' ? 'items-end' : 'items-start'}`}>
        {role === 'assistant' && agentName && (
          <span className="text-xs font-medium text-text-secondary">{agentName}</span>
        )}
        {toolCallDetails && toolCallDetails.length > 0 && (
          <div className="flex flex-col gap-2 w-full">
            {toolCallDetails.map((tc) => (
              <ToolCallCard key={tc.id} tc={tc} />
            ))}
          </div>
        )}
        {content && (
          <div
            className={`p-3 rounded-xl text-sm whitespace-pre-wrap ${
              role === 'user'
                ? 'bg-brand-50 text-text-primary'
                : 'bg-white border border-border-subtle text-text-primary'
            }`}
          >
            {content}
            {status === 'STREAMING' && (
              <span className="inline-block w-1.5 h-4 bg-brand-500 ml-0.5 animate-pulse rounded-sm" />
            )}
          </div>
        )}
        {questions && questions.length > 0 && onAnswerQuestion && (
          <div className="flex flex-col gap-2 w-full">
            {questions.map((question) => (
              <QuestionCard
                key={question.questionId}
                question={question}
                disabled={status === 'STREAMING'}
                onAnswer={onAnswerQuestion}
              />
            ))}
          </div>
        )}
        {showActions && (
          <div className={`flex items-center gap-1 ${isLastAssistant ? '' : 'opacity-0 group-hover/msg:opacity-100'} transition-opacity`}>
            <button
              onClick={handleCopy}
              className="flex items-center gap-1 px-2 py-1 rounded text-xs text-text-tertiary hover:text-brand-500 hover:bg-brand-50 transition-colors"
            >
              <Copy className="w-3 h-3" />
              {copied ? '已复制' : '复制'}
            </button>
            {onRegenerate && (
              <button
                onClick={onRegenerate}
                className="flex items-center gap-1 px-2 py-1 rounded text-xs text-text-tertiary hover:text-brand-500 hover:bg-brand-50 transition-colors"
              >
                <RefreshCw className="w-3 h-3" />
                重新生成
              </button>
            )}
          </div>
        )}
        <span className="text-xs text-text-tertiary">{formatTime(time)}</span>
      </div>
      {role === 'user' && (
        <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center shrink-0">
          <span className="text-[13px] font-semibold text-white">{avatar}</span>
        </div>
      )}
    </div>
  )
}

// ── Main Page ──

interface ToolCallDetail {
  id: string
  name: string
  status: 'running' | 'success' | 'failed' | 'requires_action'
  args?: string
  result?: string
}

interface CitationSource {
  title?: string
  url?: string
  [key: string]: unknown
}

interface LocalMessage {
  id: string
  backendId?: string
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  time: string
  avatar: string
  agentName?: string
  toolCallDetails?: ToolCallDetail[]
  questions?: QuestionCardState[]
  citations?: CitationSource[]
  status?: string
}

const parseJsonArray = (value?: string): Array<Record<string, unknown>> => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed as Array<Record<string, unknown>>
    if (parsed && typeof parsed === 'object') return [parsed as Record<string, unknown>]
  } catch {
    // ignore malformed historical payloads
  }
  return []
}

const stringValue = (value: unknown): string | undefined => {
  return typeof value === 'string' ? value : undefined
}

const booleanValue = (value: unknown): boolean => {
  return typeof value === 'boolean' ? value : value === 'true'
}

const toolStatus = (status?: string): ToolCallDetail['status'] => {
  const normalized = status?.toLowerCase()
  if (normalized === 'success') return 'success'
  if (normalized === 'requires_action') return 'requires_action'
  if (normalized === 'running') return 'running'
  return 'failed'
}

const questionFromState = (
  state: Record<string, unknown>,
  answeredToolCalls: Set<string>,
): QuestionCardState | null => {
  const questionId = stringValue(state.question_id) ?? stringValue(state.questionId)
  const toolCallId = stringValue(state.tool_call_id) ?? stringValue(state.toolCallId)
  const question = stringValue(state.question)
  const rawOptions = Array.isArray(state.options) ? state.options : []
  if (!questionId || !toolCallId || !question || rawOptions.length === 0) return null

  return {
    toolCallId,
    sessionStateId: stringValue(state.session_state_id) ?? stringValue(state.sessionStateId),
    questionId,
    question,
    options: rawOptions
      .filter((option): option is Record<string, unknown> => !!option && typeof option === 'object')
      .map((option) => ({
        id: stringValue(option.id) ?? '',
        label: stringValue(option.label) ?? '',
        description: stringValue(option.description),
      }))
      .filter((option) => option.id && option.label),
    allowFreeText: booleanValue(state.allow_free_text ?? state.allowFreeText),
    multiSelect: booleanValue(state.multi_select ?? state.multiSelect),
    status: answeredToolCalls.has(toolCallId) ? 'answered' : 'pending',
  }
}

const toLocal = (m: ChatMessageVO): LocalMessage => {
  let toolCallDetails: ToolCallDetail[] | undefined
  let questions: QuestionCardState[] | undefined
  const toolResults = parseJsonArray(m.toolResults)
  const resultByToolCall = new Map<string, Record<string, unknown>>()
  const answeredQuestionToolCalls = new Set<string>()
  for (const result of toolResults) {
    const toolCallId = stringValue(result.tool_call_id)
    if (!toolCallId) continue
    resultByToolCall.set(toolCallId, result)
    if (toolStatus(stringValue(result.status)) === 'success' && result.builtin_ui === 'question') {
      answeredQuestionToolCalls.add(toolCallId)
    }
  }

  if (m.toolCalls) {
    const parsed = parseJsonArray(m.toolCalls)
    toolCallDetails = parsed.map((t, i) => {
      const toolCallId = stringValue(t.tool_call_id) ?? `tc_${i}`
      const result = resultByToolCall.get(toolCallId)
      return {
        id: toolCallId,
        name: stringValue(t.tool_name) ?? 'unknown',
        status: result ? toolStatus(stringValue(result.status)) : 'success',
        args: stringValue(t.arguments),
        result: stringValue(result?.content),
      }
    })
  }

  const parsedQuestions = toolResults
    .map((result) => {
      const state = result.question_state
      if (!state || typeof state !== 'object') return null
      return questionFromState(state as Record<string, unknown>, answeredQuestionToolCalls)
    })
    .filter((question): question is QuestionCardState => question !== null)
  if (parsedQuestions.length > 0) {
    questions = parsedQuestions
  }

  return {
    id: m.id,
    backendId: m.id,
    role: m.role,
    content: m.content ?? '',
    time: m.createdAt,
    avatar: m.role === 'user' ? '我' : (m.agentId ?? 'A').substring(0, 1).toUpperCase(),
    agentName: m.agentId ? undefined : undefined,
    toolCallDetails,
    questions,
    status: m.status,
  }
}

const latestTodoFromMessages = (messages: ChatMessageVO[]): TodoState | null => {
  let latest: TodoState | null = null
  for (const message of messages) {
    for (const result of parseJsonArray(message.toolResults)) {
      const rawTodo = result.todo_state
      if (!rawTodo || typeof rawTodo !== 'object') continue
      const todo = rawTodo as Record<string, unknown>
      const items = Array.isArray(todo.items) ? todo.items : []
      latest = {
        title: stringValue(todo.title),
        items: items
          .filter((item): item is Record<string, unknown> => !!item && typeof item === 'object')
          .map((item) => ({
            id: stringValue(item.id) ?? '',
            title: stringValue(item.title) ?? '',
            status: (stringValue(item.status) as TodoStatus) ?? 'pending',
            detail: stringValue(item.detail),
          }))
          .filter((item) => item.id && item.title),
      }
    }
  }
  return latest
}

const latestPendingQuestion = (messages: LocalMessage[]): QuestionCardState | null => {
  for (let i = messages.length - 1; i >= 0; i -= 1) {
    const question = [...(messages[i].questions ?? [])]
      .reverse()
      .find((item) => item.status === 'pending' && item.sessionStateId)
    if (question) return question
  }
  return null
}

const questionFromSse = (data: SseQuestion): QuestionCardState => ({
  toolCallId: data.tool_call_id,
  sessionStateId: data.session_state_id,
  questionId: data.question_id,
  question: data.question,
  options: data.options ?? [],
  allowFreeText: data.allow_free_text,
  multiSelect: data.multi_select,
  status: 'pending',
})

const ChatPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const agentIdFromUrl = searchParams.get('agentId')
  const sessionIdFromUrl = searchParams.get('sessionId')
  const processedAgentIdRef = useRef<string | null>(null)
  const processedSessionIdRef = useRef<string | null>(null)

  // Session list
  const [sessions, setSessions] = useState<ChatSessionVO[]>([])
  const [sessionsLoading, setSessionsLoading] = useState(true)
  const [activeSessionId, setActiveSessionId] = useState('')
  const [deletingSessionIds, setDeletingSessionIds] = useState<Set<string>>(() => new Set())

  // Agents
  const [agents, setAgents] = useState<AgentSummaryVO[]>([])

  // Messages
  const [messages, setMessages] = useState<LocalMessage[]>([])
  const [messagesLoading, setMessagesLoading] = useState(false)

  // Input
  const [messageInput, setMessageInput] = useState('')
  const [sending, setSending] = useState(false)
  const [creatingSession, setCreatingSession] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Agent dropdown
  const [showAgentDropdown, setShowAgentDropdown] = useState(false)

  // Session search
  const [sessionSearch, setSessionSearch] = useState('')

  // Context panel
  const [showContextPanel, setShowContextPanel] = useState(() => window.innerWidth >= 1280)

  // Last message previews per session
  const [lastMessageMap, setLastMessageMap] = useState<Record<string, string>>({})

  // Built-in UI tool state
  const [activeTodo, setActiveTodo] = useState<TodoState | null>(null)
  const [pendingQuestion, setPendingQuestion] = useState<QuestionCardState | null>(null)

  // ── Fetch sessions ──

  const fetchSessions = useCallback(async () => {
    try {
      const result = await listSessions(1, 50)
      setSessions(result.data)
    } catch {
      // silent
    } finally {
      setSessionsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchSessions()
  }, [fetchSessions])

  // Fetch agents list
  useEffect(() => {
    listAgents({ page_size: 50 })
      .then((r) => setAgents(r.data))
      .catch(() => { /* ignore */ })
  }, [])

  useEffect(() => {
    const mql = window.matchMedia('(min-width: 1280px)')
    const handleViewportChange = (event: MediaQueryListEvent) => {
      setShowContextPanel(event.matches)
    }

    setShowContextPanel(mql.matches)
    mql.addEventListener('change', handleViewportChange)
    return () => mql.removeEventListener('change', handleViewportChange)
  }, [])

  // Auto-create session from ?agentId=
  useEffect(() => {
    if (!agentIdFromUrl || sessionsLoading) return
    if (sessionIdFromUrl) return
    if (processedAgentIdRef.current === agentIdFromUrl) return
    processedAgentIdRef.current = agentIdFromUrl
    ;(async () => {
      try {
        const s = await createSession({ agentId: agentIdFromUrl })
        setSessions((prev) => [s, ...prev.filter((session) => session.id !== s.id)])
        setActiveSessionId(s.id)
        setMessages([])
        setActiveTodo(null)
        setPendingQuestion(null)
      } catch {
        // ignore
      }
    })()
  }, [agentIdFromUrl, sessionIdFromUrl, sessionsLoading])

  // ── Load session detail ──

  const loadSession = useCallback(async (sessionId: string) => {
    setActiveSessionId(sessionId)
    setMessagesLoading(true)
    try {
      const detail = await getSession(sessionId)
      const localMsgs = detail.messages.map(toLocal)
      setActiveSessionId(detail.id)
      const sessionSummary: ChatSessionVO = {
        id: detail.id,
        userId: detail.userId,
        currentAgentId: detail.currentAgentId,
        title: detail.title,
        createdAt: detail.createdAt,
        updatedAt: detail.updatedAt,
      }
      setSessions((prev) =>
        prev.some((session) => session.id === detail.id)
          ? prev.map((session) => session.id === detail.id ? sessionSummary : session)
          : [sessionSummary, ...prev],
      )
      setMessages(localMsgs)
      setActiveTodo(latestTodoFromMessages(detail.messages))
      setPendingQuestion(latestPendingQuestion(localMsgs))
      const last = [...localMsgs].reverse().find((m) => m.role !== 'system' && m.content)
      if (last) {
        setLastMessageMap((prev) => ({ ...prev, [sessionId]: last.content }))
      }
    } catch {
      // ignore
    } finally {
      setMessagesLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!sessionIdFromUrl || sessionsLoading) return
    if (processedSessionIdRef.current === sessionIdFromUrl) return
    processedSessionIdRef.current = sessionIdFromUrl
    void loadSession(sessionIdFromUrl)
  }, [sessionIdFromUrl, sessionsLoading, loadSession])

  // ── Create new session ──

  const handleNewChat = async () => {
    if (creatingSession) return

    setCreatingSession(true)
    setShowAgentDropdown(false)
    try {
      let availableAgents = agents
      if (availableAgents.length === 0) {
        const result = await listAgents({ page_size: 50 })
        availableAgents = result.data
        setAgents(result.data)
      }

      const currentSessionAgentId = sessions.find((s) => s.id === activeSessionId)?.currentAgentId
      const preferredAgentId = agentIdFromUrl ?? currentSessionAgentId
      const agent = availableAgents.find((a) => a.id === preferredAgentId) ?? availableAgents[0]
      if (!agent) {
        alert('请先创建一个智能体')
        return
      }

      const session = await createSession({ agentId: agent.id })
      setSessions((prev) => [session, ...prev.filter((s) => s.id !== session.id)])
      setActiveSessionId(session.id)
      setMessages([])
      setActiveTodo(null)
      setPendingQuestion(null)
      setLastMessageMap((prev) => {
        const next = { ...prev }
        delete next[session.id]
        return next
      })
    } catch {
      alert('创建会话失败')
    } finally {
      setCreatingSession(false)
    }
  }

  const handleDeleteSession = async (sessionId: string) => {
    if (!window.confirm('确认删除此对话？')) return
    if (deletingSessionIds.has(sessionId)) return

    const isActiveDelete = activeSessionId === sessionId
    const deletedIndex = sessions.findIndex((s) => s.id === sessionId)
    const nextSession = isActiveDelete
      ? sessions[deletedIndex + 1] ?? sessions[deletedIndex - 1]
      : undefined

    setDeletingSessionIds((prev) => new Set(prev).add(sessionId))
    const removeDeletedSession = () => {
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
      setLastMessageMap((prev) => {
        const next = { ...prev }
        delete next[sessionId]
        return next
      })

      if (isActiveDelete) {
        if (nextSession) {
          loadSession(nextSession.id)
        } else {
          setActiveSessionId('')
          setMessages([])
          setActiveTodo(null)
          setPendingQuestion(null)
        }
      }
    }

    try {
      await deleteSession(sessionId)
      removeDeletedSession()
      void fetchSessions()
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        removeDeletedSession()
        void fetchSessions()
        return
      }
      alert('删除对话失败，请稍后重试')
    } finally {
      setDeletingSessionIds((prev) => {
        const next = new Set(prev)
        next.delete(sessionId)
        return next
      })
    }
  }

  const consumeAssistantStream = async (
    stream: AsyncGenerator<SseEvent>,
    assistantId: string,
  ): Promise<string> => {
    let terminalStatus = 'COMPLETED'

    for await (const event of stream) {
      let nextTodo: TodoState | null = null
      let nextQuestion: QuestionCardState | null = null

      if (event.type === 'todo_updated') {
        const todo = event.data as SseTodoUpdated
        nextTodo = {
          title: todo.title,
          items: todo.items ?? [],
        }
        setActiveTodo(nextTodo)
      }

      if (event.type === 'question') {
        nextQuestion = questionFromSse(event.data as SseQuestion)
        terminalStatus = 'WAITING_USER_INPUT'
        setPendingQuestion(nextQuestion)
      }

      if (event.type === 'message_end') {
        terminalStatus = 'COMPLETED'
        setPendingQuestion(null)
      }

      if (event.type === 'step_limit') {
        terminalStatus = 'WAITING_CONTINUE'
      }

      if (event.type === 'error') {
        const err = event.data as SseError
        if (!err.recoverable) {
          terminalStatus = 'FAILED'
        }
      }

      setMessages((prev) =>
        prev.map((m) => {
          if (m.id !== assistantId && m.backendId !== assistantId) return m
          const updated = { ...m }

          switch (event.type) {
            case 'message_start': {
              const start = event.data as SseMessageStart
              updated.backendId = start.message_id
              updated.status = 'STREAMING'
              break
            }
            case 'token': {
              const t = event.data as SseToken
              updated.content = (updated.content ?? '') + t.delta
              break
            }
            case 'tool_call_start': {
              const tc = event.data as SseToolCallStart
              const detail: ToolCallDetail = {
                id: tc.tool_call_id,
                name: tc.tool_name,
                status: 'running',
                args: tc.arguments,
              }
              updated.toolCallDetails = [...(updated.toolCallDetails ?? []), detail]
              break
            }
            case 'tool_call_end': {
              const tc = event.data as SseToolCallEnd
              updated.toolCallDetails = (updated.toolCallDetails ?? []).map((d) =>
                d.id === tc.tool_call_id
                  ? { ...d, status: toolStatus(tc.status), result: tc.result_summary }
                  : d,
              )
              break
            }
            case 'todo_updated': {
              if (nextTodo) {
                updated.status = 'STREAMING'
              }
              break
            }
            case 'question': {
              if (nextQuestion) {
                updated.questions = [
                  ...(updated.questions ?? []).filter((q) => q.questionId !== nextQuestion?.questionId),
                  nextQuestion,
                ]
                updated.status = 'WAITING_USER_INPUT'
              }
              break
            }
            case 'citation': {
              const c = event.data as SseCitation
              updated.citations = c.sources as CitationSource[]
              break
            }
            case 'step_limit': {
              const sl = event.data as SseStepLimit
              updated.content = `(已达最大步数限制 ${sl.current_step}/${sl.max_steps}，可继续执行)`
              updated.status = 'WAITING_CONTINUE'
              break
            }
            case 'message_end': {
              updated.status = 'COMPLETED'
              break
            }
            case 'error': {
              const err = event.data as SseError
              if (!err.recoverable) {
                updated.content = updated.content || `错误: ${err.message}`
                updated.status = 'FAILED'
              }
              break
            }
          }
          return updated
        }),
      )
    }

    setMessages((prev) =>
      prev.map((m) => {
        if (m.id !== assistantId && m.backendId !== assistantId) return m
        const shouldWait = terminalStatus === 'WAITING_USER_INPUT' || m.status === 'WAITING_USER_INPUT'
        return { ...m, status: shouldWait ? 'WAITING_USER_INPUT' : terminalStatus }
      }),
    )

    return terminalStatus
  }

  // ── Send message via SSE ──

  const handleSendMessage = async () => {
    const content = messageInput.trim()
    if (!content || sending) return

    let sessionId = activeSessionId
    let currentAgentName = agents.find((a) => {
      const session = sessions.find((s) => s.id === sessionId)
      return session && a.id === session.currentAgentId
    })?.name

    // Create a session if none active
    if (!sessionId) {
      try {
        const preferredAgent = agents.find((a) => a.id === agentIdFromUrl) ?? agents[0]
        if (!preferredAgent) {
          alert('请先创建一个智能体')
          return
        }
        const s = await createSession({ agentId: preferredAgent.id })
        setSessions((prev) => [s, ...prev])
        sessionId = s.id
        currentAgentName = preferredAgent.name
      } catch {
        alert('创建会话失败')
        return
      }
    }

    setMessageInput('')

    // Add user message
    const userMsg: LocalMessage = {
      id: `user_${Date.now()}`,
      role: 'user',
      content,
      time: new Date().toISOString(),
      avatar: '我',
    }
    setMessages((prev) => [...prev, userMsg])
    setActiveTodo(null)
    setPendingQuestion(null)
    setSending(true)

    // Add a placeholder assistant message that we'll update with streaming content
    const assistantId = `asst_${Date.now()}`
    const assistantMsg: LocalMessage = {
      id: assistantId,
      role: 'assistant',
      content: '',
      time: new Date().toISOString(),
      avatar: currentAgentName?.[0] ?? 'A',
      agentName: currentAgentName,
      status: 'STREAMING',
    }
    setMessages((prev) => [...prev, assistantMsg])

    try {
      const { stream } = sendMessage(sessionId, { content })
      await consumeAssistantStream(stream, assistantId)

      setMessages((prev) => {
        const finalized = prev
        const last = [...finalized].reverse().find((m) => m.role !== 'system' && m.content)
        if (last && sessionId) {
          setLastMessageMap((p) => ({ ...p, [sessionId]: last.content }))
        }
        return finalized
      })
    } catch (e) {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId
            ? { ...m, content: m.content || `发送失败: ${e instanceof Error ? e.message : '未知错误'}`, status: 'FAILED' }
            : m,
        ),
      )
    } finally {
      setSending(false)
    }
  }

  const handleRegenerate = async (messageId: string) => {
    if (!activeSessionId || sending) return
    setSending(true)

    const regenId = `regen_${Date.now()}`
    const regenMsg: LocalMessage = {
      id: regenId,
      role: 'assistant',
      content: '',
      time: new Date().toISOString(),
      avatar: currentAgent?.name?.[0] ?? 'A',
      agentName: currentAgent?.name,
      status: 'STREAMING',
    }

    setMessages((prev) => {
      const idx = prev.findIndex((m) => m.id === messageId || m.backendId === messageId)
      if (idx === -1) return [...prev, regenMsg]
      return [...prev.slice(0, idx), regenMsg]
    })

    try {
      const { stream } = regenerateMessage(activeSessionId, messageId)
      await consumeAssistantStream(stream, regenId)
    } catch (e) {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === regenId
            ? { ...m, content: m.content || `重新生成失败: ${e instanceof Error ? e.message : '未知错误'}`, status: 'FAILED' }
            : m,
        ),
      )
    } finally {
      setSending(false)
    }
  }

  const handleAnswerQuestion = async (
    question: QuestionCardState,
    selectedOptionIds?: string[],
    answerText?: string,
  ) => {
    if (!activeSessionId || sending) return
    if (!question.sessionStateId) {
      alert('该问题缺少恢复状态，请刷新会话后重试')
      return
    }

    const owner = messages.find((message) =>
      message.questions?.some((item) => item.questionId === question.questionId),
    )
    if (!owner) return

    setSending(true)
    setMessages((prev) =>
      prev.map((message) => ({
        ...message,
        questions: message.questions?.map((item) =>
          item.questionId === question.questionId
            ? {
                ...item,
                status: 'answering',
                selectedOptionIds: selectedOptionIds ?? [],
                answerText,
              }
            : item,
        ),
      })),
    )

    try {
      const { stream } = answerQuestion(activeSessionId, question.sessionStateId, {
        questionId: question.questionId,
        selectedOptionIds,
        answerText,
      })
      await consumeAssistantStream(stream, owner.id)
      setMessages((prev) =>
        prev.map((message) => ({
          ...message,
          questions: message.questions?.map((item) =>
            item.questionId === question.questionId
              ? {
                  ...item,
                  status: 'answered',
                  selectedOptionIds: selectedOptionIds ?? [],
                  answerText,
                }
              : item,
          ),
        })),
      )
    } catch (e) {
      setMessages((prev) =>
        prev.map((message) => {
          const ownsQuestion = message.questions?.some((item) => item.questionId === question.questionId)
          if (!ownsQuestion) return message
          return {
            ...message,
            content: message.content || `提交回答失败: ${e instanceof Error ? e.message : '未知错误'}`,
            questions: message.questions?.map((item) =>
              item.questionId === question.questionId ? { ...item, status: 'pending' } : item,
            ),
            status: 'FAILED',
          }
        }),
      )
      setPendingQuestion(question)
    } finally {
      setSending(false)
    }
  }

  const handleSwitchAgent = async (agentId: string, _agentName: string) => {
    setShowAgentDropdown(false)
    if (!activeSessionId) return
    try {
      await switchAgent(activeSessionId, { agentId })
      setSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionId ? { ...s, currentAgentId: agentId } : s,
        ),
      )
    } catch {
      // ignore
    }
  }

  // ── Scroll to bottom ──

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  // ── Session groups ──

  const filteredSessions = sessionSearch
    ? sessions.filter((s) =>
        (s.title ?? '').toLowerCase().includes(sessionSearch.toLowerCase()),
      )
    : sessions

  const grouped = groupSessions(filteredSessions)

  const activeSession = sessions.find((s) => s.id === activeSessionId)
  const currentAgent = agents.find((a) => a.id === activeSession?.currentAgentId)

  return (
    <div className="h-screen w-screen flex bg-bg-canvas">
      <Sidebar
        activePath={location.pathname}
        onNavigate={(path) => navigate(path)}
        onCreateClick={() => navigate('/create')}
      />

      <div className="relative flex-1 flex min-w-0">
        {/* Session List */}
        <div className="hidden bg-white border-r border-border-subtle p-3 md:flex md:w-[240px] md:flex-col md:gap-3 md:overflow-y-auto xl:w-[260px]">
          <button
            onClick={handleNewChat}
            disabled={creatingSession}
            className="flex items-center justify-center gap-2 w-full py-2.5 bg-brand-500 text-white rounded-lg font-semibold text-[13px] hover:bg-brand-600 transition-colors"
          >
            {creatingSession ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            <span>新建对话</span>
          </button>

          {/* Session Search */}
          <div className="flex items-center gap-2 px-2.5 py-2 bg-gray-100 rounded-md">
            <Search className="w-3.5 h-3.5 text-text-tertiary flex-shrink-0" />
            <input
              type="text"
              placeholder="搜索对话..."
              value={sessionSearch}
              onChange={(e) => setSessionSearch(e.target.value)}
              className="flex-1 bg-transparent text-xs outline-none placeholder:text-text-tertiary"
            />
          </div>

          {sessionsLoading && (
            <div className="flex justify-center py-8">
              <Loader2 className="w-5 h-5 animate-spin text-text-tertiary" />
            </div>
          )}

          <div className="flex flex-col gap-4">
            {grouped.map((g) => (
              <div key={g.label} className="flex flex-col gap-2">
                <span className="text-xs font-semibold text-text-tertiary px-1">{g.label}</span>
                {g.items.map((s) => (
                  <SessionItem
                    key={s.id}
                    id={s.id}
                    title={s.title ?? '新对话'}
                    agent={agents.find((a) => a.id === s.currentAgentId)?.name ?? '智能体'}
                    time={s.updatedAt}
                    lastMessage={lastMessageMap[s.id]}
                    active={activeSessionId === s.id}
                    isDeleting={deletingSessionIds.has(s.id)}
                    onClick={() => loadSession(s.id)}
                    onDelete={() => handleDeleteSession(s.id)}
                  />
                ))}
              </div>
            ))}
          </div>

          {!sessionsLoading && sessions.length === 0 && (
            <div className="text-center text-xs text-text-tertiary py-8">
              暂无对话，点击上方按钮开始
            </div>
          )}
        </div>

        {/* Chat Area */}
        <div className="flex-1 min-w-0 flex flex-col bg-bg-canvas">
          {/* Top Bar */}
          <div className="h-14 bg-white border-b border-border-subtle px-4 md:px-6 flex items-center gap-3">
            {/* Agent Selector - Pill Style */}
            <div className="relative">
              <button
                onClick={() => setShowAgentDropdown(!showAgentDropdown)}
                className="flex items-center gap-2.5 px-3 py-1.5 bg-gray-100 rounded-[20px] hover:bg-gray-200 transition-colors"
              >
                <div className="w-6 h-6 rounded-full bg-brand-500 flex items-center justify-center">
                  <Bot className="w-3.5 h-3.5 text-white" />
                </div>
                <span className="max-w-[180px] truncate text-[13px] font-semibold text-text-primary md:max-w-[220px]">
                  {currentAgent?.name ?? '选择智能体'}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-text-tertiary" />
              </button>

              {showAgentDropdown && (
                <div className="absolute top-full left-0 mt-1 w-[220px] bg-white border border-border-subtle rounded-lg shadow-lg z-10 max-h-64 overflow-y-auto">
                  {agents.map((agent) => (
                    <button
                      key={agent.id}
                      onClick={() => handleSwitchAgent(agent.id, agent.name)}
                      className={`w-full px-3 py-2.5 text-left text-sm hover:bg-gray-50 ${
                        currentAgent?.id === agent.id ? 'bg-brand-50 text-brand-500' : 'text-text-primary'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <Bot className="w-4 h-4" />
                        <span>{agent.name}</span>
                      </div>
                    </button>
                  ))}
                  {agents.length === 0 && (
                    <div className="px-3 py-4 text-xs text-text-tertiary text-center">
                      暂无可用智能体
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Session Title */}
            {activeSession && (
              <span className="min-w-0 flex-1 truncate text-sm font-medium text-text-secondary">
                {activeSession.title ?? '新对话'}
              </span>
            )}
            {!activeSession && <div className="flex-1" />}

            {/* Top Right Buttons */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowContextPanel(!showContextPanel)}
                className={`w-8 h-8 rounded-md flex items-center justify-center hover:bg-gray-100 transition-colors ${
                  showContextPanel ? 'text-brand-500' : 'text-gray-600'
                }`}
              >
                <PanelRight className="w-4 h-4" />
              </button>
              <button className="w-8 h-8 rounded-md flex items-center justify-center text-gray-600 hover:bg-gray-100 transition-colors">
                <MoreHorizontal className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Message Area */}
          <div className="flex-1 overflow-y-auto px-4 py-6 md:px-8 flex flex-col gap-5">
            {messagesLoading && (
              <div className="flex justify-center py-8">
                <Loader2 className="w-5 h-5 animate-spin text-text-tertiary" />
              </div>
            )}

            {!messagesLoading && messages.length === 0 && !activeSessionId && (
              <div className="flex-1 flex items-center justify-center">
                <div className="text-center flex flex-col gap-2">
                  <Bot className="w-12 h-12 text-text-tertiary mx-auto" />
                  <p className="text-text-tertiary text-sm">选择一个会话或创建新对话</p>
                </div>
              </div>
            )}

            {(() => {
              const lastAsstIdx = messages.reduce((acc, m, i) => m.role === 'assistant' ? i : acc, -1)
              return messages.map((msg, idx) => (
                <MessageBubble
                  key={msg.id}
                  {...msg}
                  isLastAssistant={idx === lastAsstIdx}
                  onRegenerate={msg.role === 'assistant' ? () => handleRegenerate(msg.backendId ?? msg.id) : undefined}
                  onAnswerQuestion={handleAnswerQuestion}
                />
              ))
            })()}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="px-4 pb-6 pt-4 bg-white border-t border-border-subtle md:px-6">
            <div className="flex items-end gap-2 md:gap-3">
              {/* Attach Button */}
              <button className="w-9 h-9 flex items-center justify-center rounded-lg text-text-tertiary hover:text-text-secondary hover:bg-gray-100 transition-colors flex-shrink-0">
                <Paperclip className="w-4 h-4" />
              </button>

              {/* Text Input */}
              <div className="flex-1 flex items-center gap-2 px-4 py-3 bg-white border border-border-subtle rounded-lg">
                <textarea
                  value={messageInput}
                  onChange={(e) => setMessageInput(e.target.value)}
                  placeholder={
                    activeSessionId ? '输入消息...' : '选择一个会话或创建新对话开始聊天'
                  }
                  rows={1}
                  disabled={sending}
                  className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault()
                      handleSendMessage()
                    }
                  }}
                />
                {messageInput && (
                  <button
                    onClick={() => setMessageInput('')}
                    className="text-xs text-text-tertiary hover:text-text-secondary transition-colors whitespace-nowrap"
                  >
                    清空
                  </button>
                )}
              </div>

              {/* Send Button */}
              <button
                onClick={handleSendMessage}
                disabled={!messageInput.trim() || sending}
                className={`flex items-center gap-1.5 px-4 h-9 rounded-lg transition-colors flex-shrink-0 text-sm font-medium ${
                  messageInput.trim() && !sending
                    ? 'bg-brand-500 text-white hover:bg-brand-600'
                    : 'bg-gray-100 text-gray-400'
                }`}
              >
                {sending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>发送</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Context Panel */}
        {showContextPanel && (
        <div className="absolute inset-y-0 right-0 z-20 w-[300px] max-w-full bg-white border-l border-border-subtle p-5 flex flex-col gap-5 overflow-y-auto shadow-xl xl:static xl:w-[300px] xl:shadow-none">
          <div className="flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-brand-500" />
              <span className="text-sm font-semibold text-text-primary">当前智能体</span>
            </div>

            {currentAgent ? (
              <div className="flex flex-col gap-3">
                <div className="flex items-center gap-3 p-3 bg-brand-50 rounded-lg">
                  <div className="w-10 h-10 rounded-full bg-brand-500 flex items-center justify-center">
                    <span className="text-lg font-semibold text-white">{currentAgent.name[0]}</span>
                  </div>
                  <div className="flex flex-col gap-1">
                    <span className="text-sm font-semibold text-text-primary">{currentAgent.name}</span>
                    <span className="text-xs text-text-tertiary">
                      {currentAgent.status === 'published' ? '已发布' : currentAgent.status === 'draft' ? '草稿' : '已归档'}
                    </span>
                  </div>
                </div>
                <p className="text-xs text-text-secondary">{currentAgent.description}</p>
              </div>
            ) : (
              <p className="text-xs text-text-tertiary">未选择智能体</p>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <div className="flex items-center gap-2">
              <Settings2 className="w-4 h-4 text-brand-500" />
              <span className="text-sm font-semibold text-text-primary">会话信息</span>
            </div>
            {activeSession ? (
              <div className="flex flex-col gap-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-text-tertiary">消息数</span>
                  <span className="text-text-primary">{messages.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-text-tertiary">会话ID</span>
                  <span className="text-text-primary font-mono">{activeSession.id.substring(0, 8)}...</span>
                </div>
              </div>
            ) : (
              <p className="text-xs text-text-tertiary">无活跃会话</p>
            )}
          </div>

          {activeTodo && <TodoPanel todo={activeTodo} />}

          {pendingQuestion && (
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-2">
                <HelpCircle className="w-4 h-4 text-amber-500" />
                <span className="text-sm font-semibold text-text-primary">等待用户选择</span>
              </div>
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
                <p className="text-xs font-semibold text-text-primary">{pendingQuestion.question}</p>
                <p className="mt-1 text-[11px] text-text-tertiary">
                  请在对话区的问题卡片中选择答案，Agent 会继续执行。
                </p>
              </div>
            </div>
          )}

          {(() => {
            const allToolCalls = messages.flatMap((m) => m.toolCallDetails ?? [])
            if (allToolCalls.length === 0) return null
            return (
              <div className="flex flex-col gap-3">
                <div className="flex items-center gap-2">
                  <Wrench className="w-4 h-4 text-brand-500" />
                  <span className="text-sm font-semibold text-text-primary">工具执行</span>
                </div>
                <div className="flex flex-col gap-1.5">
                  {allToolCalls.map((tc) => (
                    <div key={tc.id} className="flex items-center gap-2 px-2 py-1.5 rounded-md bg-gray-50">
                      {tc.status === 'running' ? (
                        <Loader2 className="w-3 h-3 text-brand-500 animate-spin shrink-0" />
                      ) : tc.status === 'success' ? (
                        <CheckCircle className="w-3 h-3 text-green-500 shrink-0" />
                      ) : tc.status === 'requires_action' ? (
                        <HelpCircle className="w-3 h-3 text-amber-500 shrink-0" />
                      ) : (
                        <XCircle className="w-3 h-3 text-red-500 shrink-0" />
                      )}
                      <span className="text-xs text-text-primary truncate">{tc.name}</span>
                    </div>
                  ))}
                </div>
              </div>
            )
          })()}

          {(() => {
            const allCitations = messages.flatMap((m) => m.citations ?? [])
            if (allCitations.length === 0) return null
            return (
              <div className="flex flex-col gap-3">
                <div className="flex items-center gap-2">
                  <FileText className="w-4 h-4 text-brand-500" />
                  <span className="text-sm font-semibold text-text-primary">引用来源</span>
                </div>
                <div className="flex flex-col gap-1.5">
                  {allCitations.map((c, i) => (
                    <div key={i} className="flex items-center gap-2 px-2 py-1.5 rounded-md bg-gray-50">
                      <FileText className="w-3 h-3 text-text-tertiary shrink-0" />
                      <span className="text-xs text-text-primary truncate">{c.title ?? c.url ?? `来源 ${i + 1}`}</span>
                    </div>
                  ))}
                </div>
              </div>
            )
          })()}

          {currentAgent && (
            <div className="pt-3 border-t border-border-subtle">
              <button
                onClick={() => navigate(`/agents/${currentAgent.id}`)}
                className="w-full flex items-center justify-center gap-2 py-2.5 text-brand-500 text-sm font-medium hover:bg-brand-50 rounded-md transition-colors"
              >
                <Sparkles className="w-4 h-4" />
                <span>查看智能体详情</span>
              </button>
            </div>
          )}
        </div>
        )}
      </div>
    </div>
  )
}

export default ChatPage
