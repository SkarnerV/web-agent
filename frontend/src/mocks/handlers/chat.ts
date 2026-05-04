import { http, HttpResponse, delay } from 'msw'
import { sessions, sessionMessages, getSessionDetail, agents } from '../data'
import type { ChatSessionVO, ChatSessionDetailVO, ChatMessageVO, PageResult } from '../../api/types'

const API = '/api/v1'

const lag = () => delay(Math.random() * 200 + 50)

// Store newly created sessions
const dynamicSessions: ChatSessionVO[] = []
let messageCounter = 100

export const chatHandlers = [
  // GET /api/v1/chat/sessions — list sessions
  http.get(`${API}/chat/sessions`, async ({ request }) => {
    await lag()
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const pageSize = parseInt(url.searchParams.get('page_size') || '50', 10)

    const all = [...dynamicSessions, ...sessions]
    const total = all.length
    const start = (page - 1) * pageSize
    const data = all.slice(start, start + pageSize)

    const result: PageResult<ChatSessionVO> = { data, total, page, pageSize }
    return HttpResponse.json({ success: true, data: result })
  }),

  // GET /api/v1/chat/sessions/:id — get session detail
  http.get(`${API}/chat/sessions/:id`, async ({ params }) => {
    await lag()
    const { id } = params
    try {
      const detail: ChatSessionDetailVO = getSessionDetail(id as string)
      return HttpResponse.json({ success: true, data: detail })
    } catch {
      return HttpResponse.json(
        { success: false, message: 'Session not found' },
        { status: 404 },
      )
    }
  }),

  // POST /api/v1/chat/sessions — create session
  http.post(`${API}/chat/sessions`, async ({ request }) => {
    await lag()
    const body = (await request.json()) as { agentId: string }
    const agent = agents.find((a) => a.id === body.agentId)
    const session: ChatSessionVO = {
      id: `sess-${Date.now()}`,
      userId: 'user-1',
      currentAgentId: body.agentId,
      title: `与${agent?.name ?? '智能体'}的对话`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    dynamicSessions.unshift(session)
    return HttpResponse.json({ success: true, data: session }, { status: 201 })
  }),

  // DELETE /api/v1/chat/sessions/:id/messages — clear messages
  http.delete(`${API}/chat/sessions/:id/messages`, async () => {
    await lag()
    return new HttpResponse(null, { status: 204 })
  }),

  // POST /api/v1/chat/sessions/:sessionId/switch-agent
  http.post(`${API}/chat/sessions/:sessionId/switch-agent`, async ({ params, request }) => {
    await lag()
    const { sessionId } = params
    const body = (await request.json()) as { agentId: string }
    const session = [...dynamicSessions, ...sessions].find((s) => s.id === sessionId)
    if (session) {
      session.currentAgentId = body.agentId
    }
    return new HttpResponse(null, { status: 204 })
  }),

  // POST /api/v1/chat/sessions/:sessionId/messages — send message (SSE streaming mock)
  http.post(`${API}/chat/sessions/:sessionId/messages`, async ({ params, request }) => {
    const { sessionId } = params
    const body = (await request.json()) as { content: string }

    // Add user message
    const requestId = `req-${++messageCounter}`
    const messageId = `msg-a-${++messageCounter}`

    const userMsg: ChatMessageVO = {
      id: `msg-u-${messageCounter - 1}`,
      sessionId: sessionId as string,
      role: 'user',
      content: body.content,
      status: 'COMPLETED',
      createdAt: new Date().toISOString(),
    }
    const msgs = sessionMessages[sessionId as string]
    if (msgs) msgs.push(userMsg)

    // Find agent for the session
    const session = [...dynamicSessions, ...sessions].find((s) => s.id === sessionId)
    const agent = session ? agents.find((a) => a.id === session.currentAgentId) : undefined

    // Generate mock assistant response
    let assistantResponse: string
    if (body.content.includes('分析') || body.content.includes('数据')) {
      assistantResponse = `根据你的数据分析需求，以下是 AI 模拟的响应：\n\n**关键发现**\n- 数据趋势显示稳定增长\n- 主要指标均达到预期\n\n**建议操作**\n1. 继续监控核心指标\n2. 关注异常波动\n3. 定期生成分析报告\n\n> Mock 数据 - 实际环境中会通过流式 SSE 返回实时生成的内容。`
    } else if (body.content.includes('代码') || body.content.includes('review')) {
      assistantResponse = `代码审查结果（Mock）：\n\n✅ **优点**\n- 代码结构清晰\n- 命名规范\n\n⚠️ **建议改进**\n- 添加错误处理\n- 补充单元测试\n\n> Mock 审查结果。`
    } else {
      assistantResponse = `好的，我已收到你的消息："${body.content.slice(0, 30)}..."。\n\n这是一个模拟的 AI 响应。在真实环境中，这里会通过 SSE 流式返回智能体的回复内容。\n\n你可以尝试：\n- 查看智能体的工具调用过程\n- 继续多轮对话\n- 切换不同的智能体`
    }

    // Build SSE events
    const now = new Date().toISOString()
    const events: string[] = []

    const makeEvent = (type: string, data: Record<string, unknown>) =>
      `event: ${type}\ndata: ${JSON.stringify(data)}\n`

    events.push(makeEvent('message_start', {
      request_id: requestId,
      message_id: messageId,
      agent_id: session?.currentAgentId ?? '',
      model: agent?.modelId ?? 'claude-sonnet',
      timestamp: now,
    }))

    // Simulate tool call if relevant
    if (body.content.includes('分析') || body.content.includes('数据')) {
      events.push(makeEvent('tool_call_start', {
        request_id: requestId,
        message_id: messageId,
        tool_call_id: 'tc-1',
        tool_name: 'sql_query',
        arguments: '{"query":"SELECT * FROM sales WHERE quarter=3"}',
        step_number: 1,
        timestamp: now,
      }))
      events.push(makeEvent('tool_call_end', {
        request_id: requestId,
        message_id: messageId,
        tool_call_id: 'tc-1',
        status: 'success',
        result_summary: '返回 1,245 条销售记录',
        duration_ms: 320,
        timestamp: now,
      }))
    }

    // Stream tokens character-by-character (grouped for performance)
    const tokens = assistantResponse.match(/.{1,3}/g) ?? [assistantResponse]
    let seq = 0
    for (const chunk of tokens) {
      events.push(makeEvent('token', {
        request_id: requestId,
        message_id: messageId,
        delta: chunk,
        seq: seq++,
        timestamp: now,
      }))
    }

    events.push(makeEvent('message_end', {
      request_id: requestId,
      message_id: messageId,
      finish_reason: 'stop',
      usage: { prompt_tokens: 150, completion_tokens: assistantResponse.length },
      total_steps: body.content.includes('分析') ? 3 : 1,
      timestamp: now,
    }))

    events.push('') // final empty line marks end

    const fullContent = events.join('\n')

    // Store assistant message
    const assistantMsg: ChatMessageVO = {
      id: messageId,
      sessionId: sessionId as string,
      role: 'assistant',
      content: assistantResponse,
      status: 'COMPLETED',
      agentId: session?.currentAgentId,
      modelId: agent?.modelId,
      stepCount: body.content.includes('分析') ? 3 : 1,
      createdAt: new Date().toISOString(),
    }
    if (msgs) msgs.push(assistantMsg)

    // Return SSE stream as ReadableStream
    const encoder = new TextEncoder()
    const stream = new ReadableStream({
      start(controller) {
        const chars = [...fullContent]
        let i = 0
        function push() {
          if (i >= chars.length) {
            controller.close()
            return
          }
          const chunk = chars.slice(i, i + 20).join('')
          i += 20
          controller.enqueue(encoder.encode(chunk))
          setTimeout(push, 5) // simulate streaming delay
        }
        push()
      },
    })

    return new Response(stream, {
      status: 200,
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
      },
    })
  }),

  // POST /api/v1/chat/sessions/:sessionId/messages/:messageId/regenerate
  http.post(`${API}/chat/sessions/:sessionId/messages/:messageId/regenerate`, async () => {
    await lag()
    return HttpResponse.json({
      success: true,
      data: { content: '这是重新生成后的回复（Mock）。' },
    })
  }),

  // POST /api/v1/chat/sessions/:sessionId/continue
  http.post(`${API}/chat/sessions/:sessionId/continue`, async () => {
    await lag()
    return HttpResponse.json({
      success: true,
      data: { content: '继续执行完成（Mock）。' },
    })
  }),
]
