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
} from '../api/chat'
import { listAgents } from '../api/agent'
import type {
  ChatSessionVO,
  ChatMessageVO,
  AgentSummaryVO,
  SseToken,
  SseToolCallStart,
  SseToolCallEnd,
  SseStepLimit,
  SseError,
  SseCitation,
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
    className={`w-full p-2.5 rounded-lg text-left transition-colors group relative cursor-pointer ${
      active ? 'bg-brand-50' : 'hover:bg-gray-50'
    }`}
  >
    <div className="flex flex-col gap-1">
      <span className="text-sm font-medium text-text-primary truncate pr-5">{title || '新对话'}</span>
      {lastMessage && (
        <span className="text-xs text-text-tertiary truncate block">{lastMessage}</span>
      )}
      <div className="flex items-center gap-2">
        <span className="text-xs text-text-tertiary">{agent}</span>
        <span className="text-xs text-text-tertiary">{formatTime(time)}</span>
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
    : XCircle
  const statusColor = tc.status === 'running'
    ? 'text-brand-500'
    : tc.status === 'success'
    ? 'text-green-500'
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

const MessageBubble: React.FC<{
  role: 'user' | 'assistant' | 'system' | 'tool'
  content?: string
  time: string
  avatar: string
  agentName?: string
  toolCallDetails?: ToolCallDetail[]
  status?: string
  isLastAssistant?: boolean
  onRegenerate?: () => void
}> = ({ role, content, time, avatar, agentName, toolCallDetails, status, isLastAssistant, onRegenerate }) => {
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
  status: 'running' | 'success' | 'failed'
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
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
  time: string
  avatar: string
  agentName?: string
  toolCallDetails?: ToolCallDetail[]
  citations?: CitationSource[]
  status?: string
}

const toLocal = (m: ChatMessageVO): LocalMessage => {
  let toolCallDetails: ToolCallDetail[] | undefined
  if (m.toolCalls) {
    try {
      const parsed = JSON.parse(m.toolCalls)
      toolCallDetails = (Array.isArray(parsed) ? parsed : [parsed]).map((t: { tool_call_id?: string; tool_name?: string; arguments?: string }, i: number) => ({
        id: t.tool_call_id ?? `tc_${i}`,
        name: t.tool_name ?? 'unknown',
        status: 'success' as const,
        args: t.arguments,
        result: undefined,
      }))
    } catch { /* ignore */ }
  }
  return {
    id: m.id,
    role: m.role,
    content: m.content ?? '',
    time: m.createdAt,
    avatar: m.role === 'user' ? '我' : (m.agentId ?? 'A').substring(0, 1).toUpperCase(),
    agentName: m.agentId ? undefined : undefined,
    toolCallDetails,
    status: m.status,
  }
}

const ChatPage: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const agentIdFromUrl = searchParams.get('agentId')

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
  const [showContextPanel, setShowContextPanel] = useState(true)

  // Last message previews per session
  const [lastMessageMap, setLastMessageMap] = useState<Record<string, string>>({})

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

  // Auto-create session from ?agentId=
  useEffect(() => {
    if (!agentIdFromUrl || sessionsLoading) return
    if (sessions.length > 0) return // already have sessions
    ;(async () => {
      try {
        const s = await createSession({ agentId: agentIdFromUrl })
        setSessions((prev) => [s, ...prev])
        setActiveSessionId(s.id)
      } catch {
        // ignore
      }
    })()
  }, [agentIdFromUrl, sessionsLoading, sessions.length])

  // ── Load session detail ──

  const loadSession = useCallback(async (sessionId: string) => {
    setActiveSessionId(sessionId)
    setMessagesLoading(true)
    try {
      const detail = await getSession(sessionId)
      const localMsgs = detail.messages.map(toLocal)
      setMessages(localMsgs)
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
      const agent = availableAgents.find((a) => a.id === (currentSessionAgentId ?? agentIdFromUrl)) ?? availableAgents[0]
      if (!agent) {
        alert('请先创建一个智能体')
        return
      }

      const session = await createSession({ agentId: agent.id })
      setSessions((prev) => [session, ...prev.filter((s) => s.id !== session.id)])
      setActiveSessionId(session.id)
      setMessages([])
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
        const firstAgent = agents[0]
        if (!firstAgent) {
          alert('请先创建一个智能体')
          return
        }
        const s = await createSession({ agentId: firstAgent.id })
        setSessions((prev) => [s, ...prev])
        sessionId = s.id
        currentAgentName = firstAgent.name
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

      for await (const event of stream) {
        setMessages((prev) =>
          prev.map((m) => {
            if (m.id !== assistantId) return m
            const updated = { ...m }

            switch (event.type) {
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
                    ? { ...d, status: tc.status === 'SUCCESS' ? 'success' as const : 'failed' as const, result: tc.result_summary }
                    : d,
                )
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

      setMessages((prev) => {
        const finalized = prev.map((m) => (m.id === assistantId ? { ...m, status: 'COMPLETED' } : m))
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
      const idx = prev.findIndex((m) => m.id === messageId)
      if (idx === -1) return [...prev, regenMsg]
      return [...prev.slice(0, idx), regenMsg]
    })

    try {
      const { stream } = regenerateMessage(activeSessionId, messageId)
      for await (const event of stream) {
        setMessages((prev) =>
          prev.map((m) => {
            if (m.id !== regenId) return m
            const updated = { ...m }
            switch (event.type) {
              case 'token': {
                const t = event.data as SseToken
                updated.content = (updated.content ?? '') + t.delta
                break
              }
              case 'message_end':
                updated.status = 'COMPLETED'
                break
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
        prev.map((m) => (m.id === regenId ? { ...m, status: 'COMPLETED' } : m)),
      )
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

      <div className="flex-1 flex">
        {/* Session List */}
        <div className="w-[260px] bg-white border-r border-border-subtle p-3 flex flex-col gap-3 overflow-y-auto">
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
        <div className="flex-1 flex flex-col bg-bg-canvas">
          {/* Top Bar */}
          <div className="h-14 bg-white border-b border-border-subtle px-6 flex items-center gap-3">
            {/* Agent Selector - Pill Style */}
            <div className="relative">
              <button
                onClick={() => setShowAgentDropdown(!showAgentDropdown)}
                className="flex items-center gap-2.5 px-3 py-1.5 bg-gray-100 rounded-[20px] hover:bg-gray-200 transition-colors"
              >
                <div className="w-6 h-6 rounded-full bg-brand-500 flex items-center justify-center">
                  <Bot className="w-3.5 h-3.5 text-white" />
                </div>
                <span className="text-[13px] font-semibold text-text-primary">
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
              <span className="text-sm font-medium text-text-secondary flex-1">
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
          <div className="flex-1 overflow-y-auto px-8 py-6 flex flex-col gap-5">
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
                  onRegenerate={msg.role === 'assistant' ? () => handleRegenerate(msg.id) : undefined}
                />
              ))
            })()}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="px-6 pb-6 pt-4 bg-white border-t border-border-subtle">
            <div className="flex items-end gap-3">
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
        <div className="w-[300px] bg-white border-l border-border-subtle p-5 flex flex-col gap-5 overflow-y-auto">
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
                      {currentAgent.status === 'PUBLISHED' ? '已发布' : currentAgent.status === 'DRAFT' ? '草稿' : '已归档'}
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
