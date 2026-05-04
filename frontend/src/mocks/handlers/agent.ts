import { http, HttpResponse, delay } from 'msw'
import { agents, agentVersions, agentToSummary } from '../data'
import type { AgentDetailVO, AgentVisibility, PageResult, AgentSummaryVO } from '../../api/types'

const API = '/api/v1'

// Small random delay to simulate network latency
const lag = () => delay(Math.random() * 300 + 100)

export const agentHandlers = [
  // GET /api/v1/agents — list agents
  http.get(`${API}/agents`, async ({ request }) => {
    await lag()
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const search = url.searchParams.get('search')?.toLowerCase()
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const pageSize = parseInt(url.searchParams.get('page_size') || '6', 10)

    let filtered = [...agents]

    if (status) {
      filtered = filtered.filter((a) => a.status === status)
    }
    if (search) {
      filtered = filtered.filter(
        (a) =>
          a.name.toLowerCase().includes(search) ||
          (a.description ?? '').toLowerCase().includes(search),
      )
    }

    const total = filtered.length
    const start = (page - 1) * pageSize
    const data = filtered.slice(start, start + pageSize).map(agentToSummary)

    const result: PageResult<AgentSummaryVO> = {
      data,
      total,
      page,
      pageSize,
    }

    return HttpResponse.json({ success: true, data: result })
  }),

  // GET /api/v1/agents/:id — get agent detail
  http.get(`${API}/agents/:id`, async ({ params }) => {
    await lag()
    const { id } = params
    const agent = agents.find((a) => a.id === id)
    if (!agent) {
      return HttpResponse.json(
        { success: false, message: 'Agent not found' },
        { status: 404 },
      )
    }
    return HttpResponse.json({ success: true, data: agent })
  }),

  // POST /api/v1/agents — create agent
  http.post(`${API}/agents`, async ({ request }) => {
    await lag()
    const body = (await request.json()) as Record<string, unknown>
    const newAgent: AgentDetailVO = {
      id: `agent-${Date.now()}`,
      name: (body.name as string) || '未命名智能体',
      description: (body.description as string) || '',
      avatar: (body.avatar as string) || 'bot',
      status: 'DRAFT',
      visibility: 'PRIVATE' as AgentVisibility,
      currentVersion: '1',
      hasUnpublishedChanges: true,
      ownerId: 'user-1',
      systemPrompt: (body.systemPrompt as string) || '',
      maxSteps: (body.maxSteps as number) || 10,
      modelId: (body.modelId as string) || 'claude-sonnet',
      version: 1,
      toolBindings: [],
      skillIds: [],
      knowledgeBaseIds: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    agents.unshift(newAgent)
    return HttpResponse.json({ success: true, data: newAgent }, { status: 201 })
  }),

  // PUT /api/v1/agents/:id — update agent
  http.put(`${API}/agents/:id`, async ({ params, request }) => {
    await lag()
    const { id } = params
    const idx = agents.findIndex((a) => a.id === id)
    if (idx === -1) {
      return HttpResponse.json(
        { success: false, message: 'Agent not found' },
        { status: 404 },
      )
    }
    const body = (await request.json()) as Record<string, unknown>
    agents[idx] = {
      ...agents[idx],
      ...body,
      updatedAt: new Date().toISOString(),
    } as AgentDetailVO
    return HttpResponse.json({ success: true, data: agents[idx] })
  }),

  // DELETE /api/v1/agents/:id — delete agent
  http.delete(`${API}/agents/:id`, async ({ params }) => {
    await lag()
    const { id } = params
    const idx = agents.findIndex((a) => a.id === id)
    if (idx !== -1) {
      agents.splice(idx, 1)
    }
    return new HttpResponse(null, { status: 204 })
  }),

  // POST /api/v1/agents/:id/duplicate — duplicate agent
  http.post(`${API}/agents/:id/duplicate`, async ({ params }) => {
    await lag()
    const { id } = params
    const original = agents.find((a) => a.id === id)
    if (!original) {
      return HttpResponse.json(
        { success: false, message: 'Agent not found' },
        { status: 404 },
      )
    }
    const duplicate: AgentDetailVO = {
      ...original,
      id: `agent-${Date.now()}`,
      name: `${original.name} (副本)`,
      status: 'DRAFT',
      currentVersion: '1',
      version: 1,
      hasUnpublishedChanges: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    agents.unshift(duplicate)
    return HttpResponse.json({ success: true, data: duplicate }, { status: 201 })
  }),

  // POST /api/v1/agents/import — import agent (mock)
  http.post(`${API}/agents/import`, async () => {
    await lag()
    const imported: AgentDetailVO = {
      id: `agent-${Date.now()}`,
      name: '导入的智能体',
      description: '从文件导入',
      avatar: 'bot',
      status: 'DRAFT',
      visibility: 'PRIVATE' as AgentVisibility,
      currentVersion: '1',
      hasUnpublishedChanges: true,
      ownerId: 'user-1',
      systemPrompt: '',
      maxSteps: 10,
      modelId: 'claude-sonnet',
      version: 1,
      toolBindings: [],
      skillIds: [],
      knowledgeBaseIds: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    agents.unshift(imported)
    return HttpResponse.json({
      success: true,
      data: { agent: imported, unresolvedRefs: [] },
    })
  }),

  // GET /api/v1/agents/:id/export — export agent
  http.get(`${API}/agents/:id/export`, async ({ params }) => {
    await lag()
    const { id } = params
    const agent = agents.find((a) => a.id === id)
    if (!agent) {
      return HttpResponse.json(
        { success: false, message: 'Agent not found' },
        { status: 404 },
      )
    }
    return HttpResponse.json({ success: true, data: agent })
  }),

  // GET /api/v1/agents/:id/versions — list versions
  http.get(`${API}/agents/:id/versions`, async ({ params }) => {
    await lag()
    const { id } = params
    const versions = agentVersions[id as string] ?? []
    return HttpResponse.json({ success: true, data: versions })
  }),

  // POST /api/v1/agents/:id/versions/:versionId/rollback — rollback
  http.post(`${API}/agents/:id/versions/:versionId/rollback`, async ({ params }) => {
    await lag()
    const { id } = params
    const agent = agents.find((a) => a.id === id)
    if (!agent) {
      return HttpResponse.json(
        { success: false, message: 'Agent not found' },
        { status: 404 },
      )
    }
    return HttpResponse.json({ success: true, data: agent })
  }),
]
