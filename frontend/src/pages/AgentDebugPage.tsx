import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Bot,
  Check,
  CheckCircle2,
  ChevronLeft,
  Clock,
  Loader2,
  Send,
  ShieldCheck,
  TimerReset,
  User,
  Wrench,
  XCircle,
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { getAgent } from '../api/agent'
import { answerQuestion, createSession, deleteSession, sendMessage } from '../api/chat'
import { publishAsset } from '../api/market'
import { ApiError, BASE_URL } from '../api/client'
import type {
  AgentDetailVO,
  QuestionOption,
  SseError,
  SseEvent,
  SseMessageEnd,
  SseMessageStart,
  SseQuestion,
  SseStepLimit,
  SseToken,
  SseTodoUpdated,
  SseToolCallEnd,
  SseToolCallStart,
} from '../api/types'

type LocalRole = 'user' | 'assistant'
type LocalStatus = 'SENT' | 'STREAMING' | 'COMPLETED' | 'FAILED' | 'WAITING_CONTINUE' | 'WAITING_USER_INPUT'
type ToolStatus = 'running' | 'success' | 'failed' | 'requires_action'
type StepStatus = 'pending' | 'running' | 'success' | 'failed' | 'waiting'

interface PublishConfig {
  visibility: string
  version: string
  releaseNotes?: string
}

interface DebugLocationState {
  publish?: Partial<PublishConfig>
}

interface LocalMessage {
  id: string
  backendId?: string
  role: LocalRole
  content: string
  time: string
  status: LocalStatus
  agentName?: string
}

interface ToolCallRecord {
  id: string
  name: string
  status: ToolStatus
  startedAt: string
  stepNumber?: number
  args?: string
  result?: string
  durationMs?: number
}

interface DebugStep {
  id: string
  title: string
  detail: string
  status: StepStatus
}

interface DebugQuestion {
  toolCallId: string
  sessionStateId: string
  questionId: string
  question: string
  options: QuestionOption[]
  allowFreeText: boolean
  multiSelect: boolean
  status: 'pending' | 'answering'
}

const defaultPublishConfig: PublishConfig = {
  visibility: 'public',
  version: 'v1.0.0',
}

const initialSteps: DebugStep[] = [
  {
    id: 'session',
    title: 'Step 1 · 创建调试会话',
    detail: '发送测试消息后创建临时调试会话',
    status: 'pending',
  },
  {
    id: 'message',
    title: 'Step 2 · 模拟真实请求',
    detail: '等待发送测试消息',
    status: 'pending',
  },
  {
    id: 'tools',
    title: 'Step 3 · 观察工具执行',
    detail: '暂无工具调用',
    status: 'pending',
  },
  {
    id: 'publish',
    title: 'Step 4 · 发布检查',
    detail: '完成一次成功调试后可发布',
    status: 'pending',
  },
]

function storageKey(agentId: string) {
  return `agent-platform.agent-debug-publish.${agentId}`
}

function readStoredPublishConfig(agentId?: string): Partial<PublishConfig> | null {
  if (!agentId || typeof window === 'undefined') return null
  try {
    const raw = window.sessionStorage.getItem(storageKey(agentId))
    return raw ? JSON.parse(raw) as Partial<PublishConfig> : null
  } catch {
    return null
  }
}

function normalizePublishConfig(config?: Partial<PublishConfig> | null): PublishConfig {
  const version = config?.version?.trim()
  return {
    visibility: config?.visibility || defaultPublishConfig.visibility,
    version: version || defaultPublishConfig.version,
    releaseNotes: config?.releaseNotes,
  }
}

function visibilityLabel(visibility: string) {
  const labels: Record<string, string> = {
    private: '私人',
    team: '团队',
    public: '公开',
  }
  return labels[visibility] ?? visibility
}

function formatClock(iso: string) {
  return new Date(iso).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDuration(ms: number) {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function numberValue(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function usageTokenCount(usage: Record<string, unknown>) {
  return (
    numberValue(usage.total_tokens) ??
    numberValue(usage.totalTokens) ??
    numberValue(usage.total) ??
    0
  )
}

function normalizeToolStatus(status?: string): ToolStatus {
  const normalized = status?.toLowerCase()
  if (normalized === 'success' || normalized === 'completed') return 'success'
  if (normalized === 'running' || normalized === 'pending') return 'running'
  if (normalized === 'requires_action') return 'requires_action'
  return 'failed'
}

const ChatMessage: React.FC<{ message: LocalMessage }> = ({ message }) => {
  const isAssistant = message.role === 'assistant'
  const showCursor = message.status === 'STREAMING'

  return (
    <div className={`flex gap-3 ${isAssistant ? 'justify-start' : 'justify-end'}`}>
      {isAssistant && (
        <div className="w-8 h-8 rounded-full bg-brand-500 flex items-center justify-center flex-shrink-0">
          <Bot className="w-4 h-4 text-white" />
        </div>
      )}
      <div className={`max-w-[78%] flex flex-col gap-1 ${isAssistant ? 'items-start' : 'items-end'}`}>
        {isAssistant && message.agentName && (
          <span className="text-xs font-medium text-text-secondary">{message.agentName}</span>
        )}
        <div
          className={`px-3 py-2.5 rounded-lg text-sm whitespace-pre-wrap ${
            isAssistant
              ? 'bg-white border border-border-subtle text-text-primary'
              : 'bg-brand-500 text-white'
          }`}
        >
          {message.content || (showCursor ? '思考中...' : '')}
          {showCursor && (
            <span className="ml-1 inline-block h-4 w-1 rounded-sm bg-brand-500 align-middle animate-pulse" />
          )}
        </div>
        <span className="text-xs text-text-tertiary">{formatClock(message.time)}</span>
      </div>
      {!isAssistant && (
        <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0">
          <User className="w-4 h-4 text-gray-600" />
        </div>
      )}
    </div>
  )
}

const StepRow: React.FC<{ step: DebugStep }> = ({ step }) => {
  const iconClass = 'w-4 h-4 flex-shrink-0'
  const icon = step.status === 'running'
    ? <Loader2 className={`${iconClass} text-brand-500 animate-spin`} />
    : step.status === 'success'
    ? <CheckCircle2 className={`${iconClass} text-success-500`} />
    : step.status === 'failed'
    ? <XCircle className={`${iconClass} text-error-500`} />
    : step.status === 'waiting'
    ? <Clock className={`${iconClass} text-warning-500`} />
    : <Clock className={`${iconClass} text-text-tertiary`} />

  return (
    <div className="flex gap-3 rounded-lg bg-gray-50 p-3">
      {icon}
      <div className="min-w-0 flex-1 flex flex-col gap-1">
        <span className="text-[13px] font-semibold text-text-primary">{step.title}</span>
        <span className="text-xs text-text-tertiary line-clamp-2">{step.detail}</span>
      </div>
    </div>
  )
}

const ToolCallRecordRow: React.FC<{ record: ToolCallRecord }> = ({ record }) => {
  const statusMeta = {
    running: { label: '进行中', icon: Loader2, iconColor: 'text-warning-500' },
    success: { label: '完成', icon: CheckCircle2, iconColor: 'text-success-500' },
    requires_action: { label: '待确认', icon: Clock, iconColor: 'text-warning-500' },
    failed: { label: '失败', icon: XCircle, iconColor: 'text-error-500' },
  }[record.status]
  const StatusIcon = statusMeta.icon

  return (
    <div className="rounded-lg border border-border-subtle bg-white p-3 flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <StatusIcon
          className={`w-4 h-4 ${statusMeta.iconColor} ${record.status === 'running' ? 'animate-spin' : ''}`}
        />
        <span className="min-w-0 flex-1 truncate text-[13px] font-semibold text-text-primary">
          {record.name}
        </span>
        <Badge variant={record.status === 'success' ? 'published' : record.status === 'failed' ? 'error' : 'debugging'}>
          {statusMeta.label}
        </Badge>
      </div>
      <div className="flex items-center justify-between gap-3 text-xs text-text-tertiary">
        <span>{record.stepNumber ? `第 ${record.stepNumber} 步` : '运行步骤'}</span>
        <span>{record.durationMs ? formatDuration(record.durationMs) : formatClock(record.startedAt)}</span>
      </div>
      {record.result && (
        <p className="text-xs text-text-secondary line-clamp-2">{record.result}</p>
      )}
    </div>
  )
}

const MetricCard: React.FC<{
  icon: React.ReactNode
  label: string
  value: string
}> = ({ icon, label, value }) => (
  <div className="rounded-lg bg-gray-50 p-3 flex items-center gap-3">
    <div className="w-8 h-8 rounded-lg bg-brand-50 flex items-center justify-center">
      {icon}
    </div>
    <div className="min-w-0 flex flex-col gap-0.5">
      <span className="text-xs text-text-tertiary">{label}</span>
      <span className="text-sm font-semibold text-text-primary">{value}</span>
    </div>
  </div>
)

const QuestionCard: React.FC<{
  question: DebugQuestion
  disabled?: boolean
  onAnswer: (selectedOptionIds?: string[], answerText?: string) => void
}> = ({ question, disabled, onAnswer }) => {
  const [selected, setSelected] = useState<string[]>([])
  const [freeText, setFreeText] = useState('')
  const locked = disabled || question.status === 'answering'

  const toggleOption = (id: string) => {
    if (locked) return
    if (question.multiSelect) {
      setSelected((prev) => prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id])
      return
    }
    setSelected([id])
  }

  const canSubmit = selected.length > 0 || freeText.trim().length > 0

  return (
    <div className="mx-6 mb-4 rounded-lg border border-warning-500 bg-warning-50 p-4 flex flex-col gap-3">
      <div className="flex items-start gap-2">
        <Clock className="mt-0.5 w-4 h-4 flex-shrink-0 text-warning-500" />
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-text-primary">{question.question}</p>
          <p className="mt-1 text-xs text-text-tertiary">工具调用正在等待你的回答，提交后会继续本轮调试。</p>
        </div>
      </div>
      <div className="grid grid-cols-1 gap-2">
        {question.options.map((option) => {
          const active = selected.includes(option.id)
          return (
            <button
              key={option.id}
              type="button"
              disabled={locked}
              onClick={() => toggleOption(option.id)}
              className={`rounded-md border px-3 py-2 text-left transition-colors ${
                active
                  ? 'border-warning-500 bg-white text-text-primary'
                  : 'border-warning-100 bg-white/70 text-text-secondary hover:border-warning-500'
              } disabled:cursor-not-allowed disabled:opacity-70`}
            >
              <span className="block text-xs font-semibold">{option.label}</span>
              {option.description && (
                <span className="mt-0.5 block text-xs text-text-tertiary">{option.description}</span>
              )}
            </button>
          )
        })}
      </div>
      {question.allowFreeText && (
        <textarea
          value={freeText}
          onChange={(e) => setFreeText(e.target.value)}
          disabled={locked}
          rows={2}
          placeholder="也可以补充说明..."
          className="w-full resize-none rounded-md border border-warning-100 bg-white px-3 py-2 text-sm text-text-primary outline-none placeholder:text-text-tertiary focus:border-warning-500 disabled:cursor-not-allowed disabled:opacity-70"
        />
      )}
      <Button
        variant="primary"
        size="sm"
        onClick={() => onAnswer(selected, freeText.trim() || undefined)}
        disabled={locked || !canSubmit}
        className="self-end bg-warning-500 hover:bg-amber-600"
      >
        {question.status === 'answering' ? '提交中...' : '提交回答'}
      </Button>
    </div>
  )
}

const AgentDebugPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { id } = useParams<{ id: string }>()
  const locationState = location.state as DebugLocationState | null
  const [publishConfig] = useState<PublishConfig>(() =>
    normalizePublishConfig(locationState?.publish ?? readStoredPublishConfig(id)),
  )

  const [agent, setAgent] = useState<AgentDetailVO | null>(null)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [messages, setMessages] = useState<LocalMessage[]>([])
  const [toolCalls, setToolCalls] = useState<ToolCallRecord[]>([])
  const [steps, setSteps] = useState<DebugStep[]>(initialSteps)
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [debugPassed, setDebugPassed] = useState(false)
  const [pendingQuestion, setPendingQuestion] = useState<DebugQuestion | null>(null)
  const [lastResponseMs, setLastResponseMs] = useState(0)
  const [totalTokenUsage, setTotalTokenUsage] = useState(0)
  const [runCount, setRunCount] = useState(0)
  const [successCount, setSuccessCount] = useState(0)
  const abortRef = useRef<(() => void) | null>(null)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)
  const sessionIdRef = useRef<string | null>(null)
  const cleanedSessionIdsRef = useRef<Set<string>>(new Set())

  const cleanupDebugSession = useCallback(async (targetSessionId?: string | null, updateState = true) => {
    if (!targetSessionId || cleanedSessionIdsRef.current.has(targetSessionId)) return

    cleanedSessionIdsRef.current.add(targetSessionId)
    try {
      await deleteSession(targetSessionId)
      if (sessionIdRef.current === targetSessionId) {
        sessionIdRef.current = null
        if (updateState) setSessionId(null)
      }
    } catch (e) {
      cleanedSessionIdsRef.current.delete(targetSessionId)
      console.warn('Failed to clean debug session', targetSessionId, e)
    }
  }, [])

  const cleanupDebugSessionOnUnload = useCallback(() => {
    const targetSessionId = sessionIdRef.current
    if (!targetSessionId || cleanedSessionIdsRef.current.has(targetSessionId) || typeof window === 'undefined') return

    cleanedSessionIdsRef.current.add(targetSessionId)
    const url = new URL(`${BASE_URL}/chat/sessions/${targetSessionId}`, window.location.origin)
    void fetch(url.toString(), { method: 'DELETE', keepalive: true })
  }, [])

  const metrics = useMemo(() => {
    const successRate = runCount === 0 ? '-' : `${Math.round((successCount / runCount) * 100)}%`
    return {
      responseTime: formatDuration(lastResponseMs),
      toolCallCount: String(toolCalls.length),
      tokenUsage: totalTokenUsage > 0 ? String(totalTokenUsage) : '-',
      successRate,
    }
  }, [lastResponseMs, runCount, successCount, toolCalls.length, totalTokenUsage])

  const canPublish = debugPassed && !pendingQuestion && !sending && !publishing && Boolean(agent)

  useEffect(() => {
    if (!id || typeof window === 'undefined') return
    window.sessionStorage.setItem(storageKey(id), JSON.stringify(publishConfig))
  }, [id, publishConfig])

  useEffect(() => {
    if (typeof window === 'undefined') return
    window.addEventListener('beforeunload', cleanupDebugSessionOnUnload)
    return () => window.removeEventListener('beforeunload', cleanupDebugSessionOnUnload)
  }, [cleanupDebugSessionOnUnload])

  useEffect(() => {
    if (!id) {
      setError('缺少智能体 ID')
      setLoading(false)
      return
    }

    let active = true
    ;(async () => {
      setLoading(true)
      setError(null)
      setSteps(initialSteps)
      try {
        const agentData = await getAgent(id)
        if (!active) return
        setAgent(agentData)
      } catch (e) {
        if (!active) return
        setError(e instanceof ApiError || e instanceof Error ? e.message : '调试环境初始化失败')
        setSteps((prev) =>
          prev.map((step) =>
            step.id === 'session'
              ? { ...step, detail: '调试环境初始化失败', status: 'failed' }
              : step,
          ),
        )
      } finally {
        if (active) setLoading(false)
      }
    })()

    return () => {
      active = false
      abortRef.current?.()
      void cleanupDebugSession(sessionIdRef.current, false)
    }
  }, [cleanupDebugSession, id])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const updateStep = (stepId: string, patch: Partial<DebugStep>) => {
    setSteps((prev) =>
      prev.map((step) => (step.id === stepId ? { ...step, ...patch } : step)),
    )
  }

  const updateAssistantMessage = (
    messageId: string,
    updater: (message: LocalMessage) => LocalMessage,
  ) => {
    setMessages((prev) =>
      prev.map((message) =>
        message.id === messageId || message.backendId === messageId ? updater(message) : message,
      ),
    )
  }

  const consumeAssistantStream = async (
    stream: AsyncGenerator<SseEvent>,
    assistantId: string,
    startedAt: number,
  ): Promise<{ terminal: 'completed' | 'failed' | 'waiting_continue' | 'waiting_input'; tokenUsage: number }> => {
    let terminal: 'completed' | 'failed' | 'waiting_continue' | 'waiting_input' = 'completed'
    let tokenUsage = 0
    let sawToolCall = false
    let hadBlockingFailure = false

    for await (const event of stream) {
      switch (event.type) {
        case 'message_start': {
          const start = event.data as SseMessageStart
          updateAssistantMessage(assistantId, (message) => ({
            ...message,
            backendId: start.message_id,
            status: 'STREAMING',
          }))
          updateStep('message', {
            detail: `模型 ${start.model || agent?.modelId || 'default'} 正在输出`,
            status: 'running',
          })
          break
        }
        case 'token': {
          const token = event.data as SseToken
          updateAssistantMessage(assistantId, (message) => ({
            ...message,
            content: `${message.content}${token.delta}`,
          }))
          break
        }
        case 'tool_call_start': {
          const tool = event.data as SseToolCallStart
          sawToolCall = true
          setToolCalls((prev) => [
            ...prev,
            {
              id: tool.tool_call_id,
              name: tool.tool_name,
              status: 'running',
              startedAt: new Date().toISOString(),
              stepNumber: tool.step_number,
              args: tool.arguments,
            },
          ])
          updateStep('tools', {
            detail: `正在调用 ${tool.tool_name}`,
            status: 'running',
          })
          break
        }
        case 'tool_call_end': {
          const tool = event.data as SseToolCallEnd
          const status = normalizeToolStatus(tool.status)
          setToolCalls((prev) =>
            prev.map((record) =>
              record.id === tool.tool_call_id
                ? {
                    ...record,
                    status,
                    result: tool.result_summary,
                    durationMs: tool.duration_ms,
                  }
                : record,
            ),
          )
          updateStep('tools', {
            detail: status === 'requires_action'
              ? `${tool.tool_call_id.slice(0, 8)} 等待用户确认`
              : `${tool.tool_call_id.slice(0, 8)} ${status === 'success' ? '执行完成' : '执行失败'}`,
            status: status === 'success' ? 'success' : status === 'requires_action' ? 'waiting' : 'failed',
          })
          if (status === 'failed') {
            hadBlockingFailure = true
            terminal = 'failed'
          }
          break
        }
        case 'todo_updated': {
          const todo = event.data as SseTodoUpdated
          updateStep('message', {
            detail: todo.title ? `任务清单：${todo.title}` : `${todo.items?.length ?? 0} 个任务正在推进`,
            status: 'running',
          })
          break
        }
        case 'step_limit': {
          const limit = event.data as SseStepLimit
          terminal = 'waiting_continue'
          hadBlockingFailure = true
          updateAssistantMessage(assistantId, (message) => ({
            ...message,
            content: message.content || `已达到最大步数 ${limit.current_step}/${limit.max_steps}`,
            status: 'WAITING_CONTINUE',
          }))
          updateStep('message', {
            detail: `已达到最大步数 ${limit.current_step}/${limit.max_steps}`,
            status: 'failed',
          })
          break
        }
        case 'question': {
          const question = event.data as SseQuestion
          terminal = 'waiting_input'
          setPendingQuestion({
            toolCallId: question.tool_call_id,
            sessionStateId: question.session_state_id,
            questionId: question.question_id,
            question: question.question,
            options: question.options ?? [],
            allowFreeText: question.allow_free_text,
            multiSelect: question.multi_select,
            status: 'pending',
          })
          updateAssistantMessage(assistantId, (message) => ({
            ...message,
            content: message.content || `需要人工确认：${question.question}`,
            status: 'WAITING_USER_INPUT',
          }))
          updateStep('message', {
            detail: '智能体请求人工确认',
            status: 'waiting',
          })
          updateStep('publish', {
            detail: '回答问题后继续调试',
            status: 'waiting',
          })
          break
        }
        case 'message_end': {
          const end = event.data as SseMessageEnd
          tokenUsage = usageTokenCount(end.usage ?? {})
          const passed = !hadBlockingFailure
          updateAssistantMessage(assistantId, (message) => ({
            ...message,
            status: 'COMPLETED',
          }))
          updateStep('message', {
            detail: passed ? `输出完成，共 ${end.total_steps} 个步骤` : '输出完成，但本轮存在异常',
            status: passed ? 'success' : 'failed',
          })
          if (!sawToolCall) {
            updateStep('tools', {
              detail: '本轮未触发工具调用',
              status: 'success',
            })
          }
          updateStep('publish', {
            detail: passed ? '调试通过，可以发布' : '本轮调试未通过',
            status: passed ? 'success' : 'failed',
          })
          terminal = passed ? 'completed' : terminal
          break
        }
        case 'error': {
          const err = event.data as SseError
          if (!err.recoverable) {
            terminal = 'failed'
            hadBlockingFailure = true
            updateAssistantMessage(assistantId, (message) => ({
              ...message,
              content: message.content || `错误：${err.message}`,
              status: 'FAILED',
            }))
            updateStep('message', {
              detail: err.message,
              status: 'failed',
            })
          }
          break
        }
      }
    }

    setLastResponseMs(Date.now() - startedAt)
    return { terminal, tokenUsage }
  }

  const ensureSession = async () => {
    if (sessionId) return sessionId
    if (!id) throw new Error('缺少智能体 ID')
    updateStep('session', {
      detail: '正在创建临时调试会话',
      status: 'running',
    })
    const session = await createSession({ agentId: id })
    sessionIdRef.current = session.id
    setSessionId(session.id)
    updateStep('session', {
      detail: `会话 ${session.id.slice(0, 8)} 已就绪`,
      status: 'success',
    })
    return session.id
  }

  const handleSendMessage = async () => {
    const content = inputValue.trim()
    if (!content || sending || !agent) return

    setSending(true)
    setInputValue('')
    setError(null)
    setDebugPassed(false)
    setPendingQuestion(null)
    setToolCalls([])
    setSteps((prev) =>
      prev.map((step) => {
        if (step.id === 'message') return { ...step, detail: '测试消息已发送', status: 'running' }
        if (step.id === 'tools') return { ...step, detail: '等待工具调用', status: 'pending' }
        if (step.id === 'publish') return { ...step, detail: '等待本轮调试结果', status: 'pending' }
        return step
      }),
    )

    const now = new Date().toISOString()
    const assistantId = `debug_asst_${Date.now()}`
    setMessages((prev) => [
      ...prev,
      {
        id: `debug_user_${Date.now()}`,
        role: 'user',
        content,
        time: now,
        status: 'SENT',
      },
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        time: now,
        status: 'STREAMING',
        agentName: agent.name,
      },
    ])

    const startedAt = Date.now()
    let activeSessionId: string | null = null
    try {
      activeSessionId = await ensureSession()
      const { abort, stream } = sendMessage(activeSessionId, {
        content,
        idempotencyKey: `debug-${activeSessionId}-${startedAt}`,
      })
      abortRef.current = abort
      const result = await consumeAssistantStream(stream, assistantId, startedAt)
      const passed = result.terminal === 'completed'
      setDebugPassed(passed)
      setTotalTokenUsage((prev) => prev + result.tokenUsage)
      if (result.terminal !== 'waiting_input') {
        setRunCount((prev) => prev + 1)
        setSuccessCount((prev) => prev + (passed ? 1 : 0))
      }
      if (!passed) {
        updateStep('publish', {
          detail: result.terminal === 'waiting_continue'
            ? '需要继续运行后再发布'
            : result.terminal === 'waiting_input'
            ? '回答问题后继续调试'
            : '本轮调试未通过',
          status: result.terminal === 'waiting_input' ? 'waiting' : 'failed',
        })
      }
      if (result.terminal !== 'waiting_input') {
        await cleanupDebugSession(activeSessionId)
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : '发送失败'
      setError(message)
      setRunCount((prev) => prev + 1)
      setLastResponseMs(Date.now() - startedAt)
      updateAssistantMessage(assistantId, (assistant) => ({
        ...assistant,
        content: assistant.content || `发送失败：${message}`,
        status: 'FAILED',
      }))
      updateStep('message', {
        detail: message,
        status: 'failed',
      })
      updateStep('publish', {
        detail: '请修复问题后重新调试',
        status: 'failed',
      })
      await cleanupDebugSession(activeSessionId)
    } finally {
      abortRef.current = null
      setSending(false)
    }
  }

  const handleAnswerQuestion = async (selectedOptionIds?: string[], answerText?: string) => {
    if (!sessionId || !pendingQuestion || sending || !agent) return

    setPendingQuestion((prev) => prev ? { ...prev, status: 'answering' } : prev)
    setSending(true)
    setError(null)

    const answerLabel = answerText
      || pendingQuestion.options
        .filter((option) => selectedOptionIds?.includes(option.id))
        .map((option) => option.label)
        .join('、')
      || '已回答'
    const now = new Date().toISOString()
    const assistantId = `debug_asst_${Date.now()}`

    setMessages((prev) => [
      ...prev,
      {
        id: `debug_answer_${Date.now()}`,
        role: 'user',
        content: answerLabel,
        time: now,
        status: 'SENT',
      },
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        time: now,
        status: 'STREAMING',
        agentName: agent.name,
      },
    ])
    updateStep('message', {
      detail: '已提交回答，继续运行',
      status: 'running',
    })

    const startedAt = Date.now()
    const activeSessionId = sessionId
    try {
      const { abort, stream } = answerQuestion(activeSessionId, pendingQuestion.sessionStateId, {
        questionId: pendingQuestion.questionId,
        selectedOptionIds,
        answerText,
      })
      abortRef.current = abort
      setPendingQuestion(null)
      const result = await consumeAssistantStream(stream, assistantId, startedAt)
      const passed = result.terminal === 'completed'
      setDebugPassed(passed)
      setTotalTokenUsage((prev) => prev + result.tokenUsage)
      setRunCount((prev) => prev + 1)
      setSuccessCount((prev) => prev + (passed ? 1 : 0))
      if (passed) {
        setToolCalls((prev) =>
          prev.map((record) =>
            record.id === pendingQuestion.toolCallId
              ? { ...record, status: 'success', result: 'User answered the question' }
              : record,
          ),
        )
        updateStep('tools', {
          detail: 'question 工具已完成',
          status: 'success',
        })
      } else {
        updateStep('publish', {
          detail: result.terminal === 'waiting_input' ? '回答问题后继续调试' : '本轮调试未通过',
          status: result.terminal === 'waiting_input' ? 'waiting' : 'failed',
        })
      }
      if (result.terminal !== 'waiting_input') {
        await cleanupDebugSession(activeSessionId)
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : '提交回答失败'
      setError(message)
      setRunCount((prev) => prev + 1)
      setLastResponseMs(Date.now() - startedAt)
      setPendingQuestion((prev) => prev ? { ...prev, status: 'pending' } : pendingQuestion)
      updateAssistantMessage(assistantId, (assistant) => ({
        ...assistant,
        content: assistant.content || `提交回答失败：${message}`,
        status: 'FAILED',
      }))
      updateStep('message', {
        detail: message,
        status: 'failed',
      })
      updateStep('publish', {
        detail: '请重新提交回答或重新调试',
        status: 'failed',
      })
      setPendingQuestion(null)
      await cleanupDebugSession(activeSessionId)
    } finally {
      abortRef.current = null
      setSending(false)
    }
  }

  const handlePassAndPublish = async () => {
    if (!id || !canPublish) return
    setPublishing(true)
    setError(null)
    try {
      await publishAsset({
        assetType: 'AGENT',
        assetId: id,
        visibility: publishConfig.visibility,
        version: publishConfig.version,
        releaseNotes: publishConfig.releaseNotes?.trim() || undefined,
      })
      if (typeof window !== 'undefined') {
        window.sessionStorage.removeItem(storageKey(id))
      }
      await cleanupDebugSession(sessionIdRef.current, false)
      navigate('/agents', { state: { published: true, agentId: id } })
    } catch (e) {
      setError(e instanceof ApiError || e instanceof Error ? e.message : '发布失败，请重试')
      updateStep('publish', {
        detail: '发布失败，请重试',
        status: 'failed',
      })
    } finally {
      setPublishing(false)
    }
  }

  const handleBack = async () => {
    await cleanupDebugSession(sessionIdRef.current, false)
    navigate(id ? `/agents/edit/${id}` : '/agents')
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '调试' }]}>
      <div className="h-14 bg-white border-b border-border-subtle flex items-center justify-between px-6">
        <button
          onClick={handleBack}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回编辑</span>
        </button>
        <div className="min-w-0 flex flex-1 items-center justify-center gap-2 px-4">
          <span className="truncate text-sm font-semibold text-text-primary">
            调试：{agent?.name ?? '智能体'}
          </span>
          <Badge variant={debugPassed ? 'published' : 'debugging'}>
            {debugPassed ? '已通过' : '调试中'}
          </Badge>
        </div>
        <Button
          variant="primary"
          icon={publishing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
          onClick={handlePassAndPublish}
          disabled={!canPublish}
          className="bg-success-500 hover:bg-green-600"
        >
          {publishing ? '发布中...' : '通过并发布'}
        </Button>
      </div>

      <div className="flex-1 flex overflow-hidden">
        <section className="min-w-0 flex-1 flex flex-col bg-gray-50">
          <div className="mx-6 my-4 rounded-lg border border-warning-500 bg-warning-50 px-3 py-2.5 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 flex-shrink-0 text-warning-500" />
            <span className="text-sm text-warning-500">
              当前处于发布前调试模式，只有完成一次成功调试后才可发布。
            </span>
          </div>

          {error && (
            <div className="mx-6 mb-4 rounded-lg border border-error-500 bg-error-50 px-3 py-2 text-sm text-error-500">
              {error}
            </div>
          )}

          <div className="flex-1 overflow-y-auto px-6 pb-6 flex flex-col gap-4">
            {loading && (
              <div className="flex flex-1 items-center justify-center">
                <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
              </div>
            )}

            {!loading && messages.length === 0 && (
              <div className="flex flex-1 items-center justify-center">
                <div className="flex flex-col items-center gap-3 text-center">
                  <div className="w-12 h-12 rounded-full bg-brand-50 flex items-center justify-center">
                    <Bot className="w-6 h-6 text-brand-500" />
                  </div>
                  <span className="text-sm font-medium text-text-secondary">暂无调试消息</span>
                </div>
              </div>
            )}

            {messages.map((message) => (
              <ChatMessage key={message.id} message={message} />
            ))}
            <div ref={messagesEndRef} />
          </div>

          {pendingQuestion && (
            <QuestionCard
              question={pendingQuestion}
              disabled={sending}
              onAnswer={handleAnswerQuestion}
            />
          )}

          <div className="border-t border-border-subtle bg-white px-6 py-4">
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    handleSendMessage()
                  }
                }}
                disabled={loading || sending || !agent || Boolean(pendingQuestion)}
                placeholder="输入消息进行测试..."
                className="min-w-0 flex-1 rounded-md border border-border-subtle bg-gray-50 px-3 py-2.5 text-sm text-text-primary outline-none placeholder:text-text-tertiary focus:border-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
              />
              <Button
                variant="primary"
                icon={sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                onClick={handleSendMessage}
                disabled={!inputValue.trim() || sending || loading || !agent || Boolean(pendingQuestion)}
              >
                发送
              </Button>
            </div>
          </div>
        </section>

        <aside className="w-[380px] flex-shrink-0 border-l border-border-subtle bg-white p-5 overflow-y-auto flex flex-col gap-5">
          <div className="flex items-center gap-2">
            <Activity className="w-4 h-4 text-brand-500" />
            <span className="text-sm font-semibold text-text-primary">运行过程监控</span>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <MetricCard
              icon={<TimerReset className="w-4 h-4 text-brand-500" />}
              label="响应时间"
              value={metrics.responseTime}
            />
            <MetricCard
              icon={<Wrench className="w-4 h-4 text-brand-500" />}
              label="工具调用"
              value={metrics.toolCallCount}
            />
            <MetricCard
              icon={<BarChart3 className="w-4 h-4 text-brand-500" />}
              label="Token 使用"
              value={metrics.tokenUsage}
            />
            <MetricCard
              icon={<ShieldCheck className="w-4 h-4 text-success-500" />}
              label="成功率"
              value={metrics.successRate}
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-text-tertiary">执行步骤</span>
            {steps.map((step) => (
              <StepRow key={step.id} step={step} />
            ))}
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-xs font-semibold text-text-tertiary">工具调用记录</span>
            {toolCalls.length === 0 ? (
              <div className="rounded-lg bg-gray-50 p-3 text-xs text-text-tertiary">
                暂无工具调用
              </div>
            ) : (
              toolCalls.map((record) => (
                <ToolCallRecordRow key={record.id} record={record} />
              ))
            )}
          </div>

          <div className="rounded-lg border border-border-subtle bg-gray-50 p-3 flex flex-col gap-2">
            <span className="text-xs font-semibold text-text-tertiary">发布设置</span>
            <div className="flex items-center justify-between gap-3 text-xs">
              <span className="text-text-tertiary">可见范围</span>
              <span className="font-medium text-text-primary">{visibilityLabel(publishConfig.visibility)}</span>
            </div>
            <div className="flex items-center justify-between gap-3 text-xs">
              <span className="text-text-tertiary">版本号</span>
              <span className="font-medium text-text-primary">{publishConfig.version}</span>
            </div>
            {publishConfig.releaseNotes && (
              <p className="text-xs text-text-secondary line-clamp-3">{publishConfig.releaseNotes}</p>
            )}
          </div>
        </aside>
      </div>
    </Layout>
  )
}

export default AgentDebugPage
