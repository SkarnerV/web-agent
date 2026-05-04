import type {
  AgentSummaryVO,
  AgentDetailVO,
  AgentStatus,
  AgentVisibility,
  ChatSessionVO,
  ChatSessionDetailVO,
  ChatMessageVO,
  AssetVersionVO,
} from '../api/types'

// ── Agents ──

export const agents: AgentDetailVO[] = [
  {
    id: 'agent-1',
    name: '客服机器人',
    description:
      '24小时智能客服助手，支持多轮对话、意图识别、知识库检索，可处理常见问题并自动升级复杂工单。',
    avatar: 'bot',
    status: 'PUBLISHED' as AgentStatus,
    visibility: 'WORKSPACE' as AgentVisibility,
    currentVersion: '3',
    hasUnpublishedChanges: false,
    ownerId: 'user-1',
    systemPrompt:
      '你是一个专业的客服助手。请礼貌、耐心地回答用户问题。如果遇到无法解决的问题，请引导用户联系人工客服。',
    maxSteps: 10,
    modelId: 'claude-sonnet',
    version: 3,
    toolBindings: [
      { id: 'tb-1', sourceType: 'mcp', sourceId: 'mcp-1', toolName: 'knowledge_search', enabled: true, sortOrder: 1 },
      { id: 'tb-2', sourceType: 'skill', sourceId: 'skill-1', toolName: 'ticket_create', enabled: true, sortOrder: 2 },
    ],
    skillIds: ['skill-1'],
    knowledgeBaseIds: ['kb-1'],
    createdAt: '2026-04-15T08:00:00Z',
    updatedAt: '2026-05-03T14:22:00Z',
  },
  {
    id: 'agent-2',
    name: '数据分析助手',
    description: '支持多种数据源分析，自动生成可视化图表和分析报告，帮助快速洞察业务趋势。',
    avatar: 'sparkles',
    status: 'PUBLISHED' as AgentStatus,
    visibility: 'WORKSPACE' as AgentVisibility,
    currentVersion: '2',
    hasUnpublishedChanges: false,
    ownerId: 'user-1',
    systemPrompt: '你是一个数据分析专家。请根据用户提供的数据进行分析，并给出可操作的洞察建议。使用图表和表格来清晰地展示结果。',
    maxSteps: 15,
    modelId: 'claude-opus',
    version: 2,
    toolBindings: [
      { id: 'tb-3', sourceType: 'mcp', sourceId: 'mcp-2', toolName: 'sql_query', enabled: true, sortOrder: 1 },
      { id: 'tb-4', sourceType: 'mcp', sourceId: 'mcp-3', toolName: 'chart_generate', enabled: true, sortOrder: 2 },
    ],
    skillIds: ['skill-2'],
    knowledgeBaseIds: [],
    createdAt: '2026-04-20T10:00:00Z',
    updatedAt: '2026-05-02T09:15:00Z',
  },
  {
    id: 'agent-3',
    name: '代码审查专家',
    description: '精准识别代码问题，支持多语言审查与优化建议，帮助团队提升代码质量。',
    avatar: 'zap',
    status: 'PUBLISHED' as AgentStatus,
    visibility: 'WORKSPACE' as AgentVisibility,
    currentVersion: '5',
    hasUnpublishedChanges: true,
    ownerId: 'user-1',
    systemPrompt: '你是一个资深代码审查专家。请审查用户提供的代码，找出潜在的问题、安全隐患和性能瓶颈，并提供具体的改进建议。',
    maxSteps: 20,
    modelId: 'claude-opus',
    version: 5,
    toolBindings: [
      { id: 'tb-5', sourceType: 'mcp', sourceId: 'mcp-4', toolName: 'github_api', enabled: true, sortOrder: 1 },
      { id: 'tb-6', sourceType: 'skill', sourceId: 'skill-3', toolName: 'lint_check', enabled: true, sortOrder: 2 },
    ],
    skillIds: ['skill-3'],
    knowledgeBaseIds: ['kb-2'],
    createdAt: '2026-03-10T16:00:00Z',
    updatedAt: '2026-05-01T11:30:00Z',
  },
  {
    id: 'agent-4',
    name: '翻译专家',
    description: '高质量中英互译智能体，支持技术文档、代码注释等专业内容翻译，保持术语一致性。',
    avatar: 'message',
    status: 'PUBLISHED' as AgentStatus,
    visibility: 'PRIVATE' as AgentVisibility,
    currentVersion: '1',
    hasUnpublishedChanges: false,
    ownerId: 'user-1',
    systemPrompt: '你是一个专业翻译。请准确翻译用户提供的文本，注意保持技术术语的准确性。',
    maxSteps: 5,
    modelId: 'claude-sonnet',
    version: 1,
    toolBindings: [],
    skillIds: [],
    knowledgeBaseIds: ['kb-3'],
    createdAt: '2026-05-01T08:00:00Z',
    updatedAt: '2026-05-01T08:00:00Z',
  },
  {
    id: 'agent-5',
    name: '需求文档助手',
    description: '帮助产品经理快速撰写和整理需求文档，支持多种模板格式，自动生成用户故事和验收标准。',
    avatar: 'bot',
    status: 'DRAFT' as AgentStatus,
    visibility: 'PRIVATE' as AgentVisibility,
    currentVersion: '1',
    hasUnpublishedChanges: true,
    ownerId: 'user-1',
    systemPrompt: '你是一个产品需求分析专家。请帮助用户将模糊的想法转化为清晰的需求文档。',
    maxSteps: 10,
    modelId: 'claude-sonnet',
    version: 1,
    toolBindings: [],
    skillIds: [],
    knowledgeBaseIds: [],
    createdAt: '2026-05-03T09:00:00Z',
    updatedAt: '2026-05-04T07:10:00Z',
  },
  {
    id: 'agent-6',
    name: '项目管理助手',
    description: '任务分解、进度追踪、资源分配建议，帮助团队高效协作。',
    avatar: 'zap',
    status: 'ARCHIVED' as AgentStatus,
    visibility: 'WORKSPACE' as AgentVisibility,
    currentVersion: '2',
    hasUnpublishedChanges: false,
    ownerId: 'user-1',
    systemPrompt: '你是一个项目管理专家。帮助用户分解任务、规划时间和资源。',
    maxSteps: 8,
    modelId: 'claude-sonnet',
    version: 2,
    toolBindings: [],
    skillIds: [],
    knowledgeBaseIds: [],
    createdAt: '2026-02-01T10:00:00Z',
    updatedAt: '2026-03-15T14:00:00Z',
  },
  {
    id: 'agent-7',
    name: '会议纪要官',
    description: '自动整理会议内容，生成结构化会议纪要，提取行动项和决策要点。',
    avatar: 'sparkles',
    status: 'PUBLISHED' as AgentStatus,
    visibility: 'WORKSPACE' as AgentVisibility,
    currentVersion: '1',
    hasUnpublishedChanges: false,
    ownerId: 'user-2',
    systemPrompt: '你是一个会议记录专家。请根据会议内容，提取关键讨论点、决策和行动项。',
    maxSteps: 5,
    modelId: 'claude-sonnet',
    version: 1,
    toolBindings: [],
    skillIds: [],
    knowledgeBaseIds: [],
    createdAt: '2026-04-25T13:00:00Z',
    updatedAt: '2026-04-25T13:00:00Z',
  },
]

// ── Agent Versions ──

export const agentVersions: Record<string, AssetVersionVO[]> = {
  'agent-1': [
    { id: 'v1-1', assetType: 'agent', assetId: 'agent-1', version: '1', releaseNotes: '初始版本', publishedBy: '张三', publishedAt: '2026-04-15T08:00:00Z' },
    { id: 'v1-2', assetType: 'agent', assetId: 'agent-1', version: '2', releaseNotes: '新增知识库检索工具', publishedBy: '张三', publishedAt: '2026-04-22T10:00:00Z' },
    { id: 'v1-3', assetType: 'agent', assetId: 'agent-1', version: '3', releaseNotes: '优化多轮对话能力', publishedBy: '张三', publishedAt: '2026-05-01T09:00:00Z' },
  ],
  'agent-2': [
    { id: 'v2-1', assetType: 'agent', assetId: 'agent-2', version: '1', releaseNotes: '初始版本', publishedBy: '张三', publishedAt: '2026-04-20T10:00:00Z' },
    { id: 'v2-2', assetType: 'agent', assetId: 'agent-2', version: '2', releaseNotes: '新增图表生成功能', publishedBy: '张三', publishedAt: '2026-04-28T16:00:00Z' },
  ],
}

// ── Chat Sessions ──

const now = new Date()

function ago(minutes: number): string {
  return new Date(now.getTime() - minutes * 60000).toISOString()
}

export const sessions: ChatSessionVO[] = [
  { id: 'sess-1', userId: 'user-1', currentAgentId: 'agent-2', title: '关于 Q3 销售数据分析', createdAt: ago(45), updatedAt: ago(5) },
  { id: 'sess-2', userId: 'user-1', currentAgentId: 'agent-1', title: '帮我总结这篇文档', createdAt: ago(180), updatedAt: ago(120) },
  { id: 'sess-3', userId: 'user-1', currentAgentId: 'agent-3', title: 'Python 装饰器用法', createdAt: ago(1500), updatedAt: ago(1440) },
  { id: 'sess-4', userId: 'user-1', currentAgentId: 'agent-7', title: '周会纪要整理', createdAt: ago(3000), updatedAt: ago(2880) },
  { id: 'sess-5', userId: 'user-1', currentAgentId: 'agent-2', title: '用户留存率趋势', createdAt: ago(5000), updatedAt: ago(4320) },
]

// ── Chat Messages ──

export const sessionMessages: Record<string, ChatMessageVO[]> = {
  'sess-1': [
    {
      id: 'msg-1-1', sessionId: 'sess-1', role: 'user',
      content: '帮我分析 Q3 的销售数据，重点关注团队业绩增长趋势',
      status: 'COMPLETED', agentId: 'agent-2', createdAt: ago(45),
    },
    {
      id: 'msg-1-2', sessionId: 'sess-1', role: 'tool',
      content: '调用工具: sql_query', status: 'COMPLETED',
      toolCalls: JSON.stringify([{ tool_name: 'sql_query', arguments: '{"query":"SELECT * FROM sales WHERE quarter=3"}' }]),
      toolResults: '返回 1,245 条销售记录', createdAt: ago(40),
    },
    {
      id: 'msg-1-3', sessionId: 'sess-1', role: 'assistant',
      content: '根据 Q3 销售数据分析，以下是关键发现：\n\n**整体表现**\n- 总销售额：¥8,500,000，同比增长 23%\n- 订单量：1,245 单，环比增长 15%\n\n**团队排名**\n1. 张伟团队 — ¥2,800,000（↑32%）\n2. 李娜团队 — ¥2,100,000（↑18%）\n3. 王强团队 — ¥1,900,000（↑25%）\n\n**趋势洞察**\n- 7月销售额较低（¥2.4M），8月开始反弹，9月达到峰值（¥3.2M）\n- 企业客户占比从 Q2 的 45% 提升到 58%\n\n需要进一步分析哪个维度吗？',
      status: 'COMPLETED', agentId: 'agent-2', modelId: 'claude-opus', stepCount: 3, createdAt: ago(35),
    },
    {
      id: 'msg-1-4', sessionId: 'sess-1', role: 'user',
      content: '好的，让我看看具体数据', status: 'COMPLETED', agentId: 'agent-2', createdAt: ago(5),
    },
    {
      id: 'msg-1-5', sessionId: 'sess-1', role: 'assistant',
      content: '好的，以下是详细数据：\n\n| 月份 | 销售额 | 订单量 | 客单价 |\n|------|--------|--------|--------|\n| 7月  | ¥2.4M  | 380    | ¥6,316 |\n| 8月  | ¥2.9M  | 420    | ¥6,905 |\n| 9月  | ¥3.2M  | 445    | ¥7,191 |\n\n可以看到，不仅订单量在增长，客单价也在稳步提升，说明客户质量在改善。',
      status: 'COMPLETED', agentId: 'agent-2', modelId: 'claude-opus', stepCount: 2, createdAt: ago(4),
    },
  ],
  'sess-2': [
    {
      id: 'msg-2-1', sessionId: 'sess-2', role: 'user',
      content: '帮我总结一下这篇产品需求文档', status: 'COMPLETED', createdAt: ago(180),
    },
    {
      id: 'msg-2-2', sessionId: 'sess-2', role: 'assistant',
      content: '好的，我来总结这篇产品需求文档的核心要点：\n\n**产品概述**\n这是一个面向企业客户的智能客服平台，目标是降低人工客服成本 40%。\n\n**核心功能**\n1. 多渠道接入（Web、微信、APP）\n2. AI 自动回复 + 人工接管\n3. 知识库管理与自动学习\n4. 数据分析看板\n\n**关键里程碑**\n- MVP：2026 Q2\n- 正式版：2026 Q4\n\n需要我详细展开某个部分吗？',
      status: 'COMPLETED', agentId: 'agent-1', createdAt: ago(175),
    },
  ],
  'sess-3': [
    {
      id: 'msg-3-1', sessionId: 'sess-3', role: 'user',
      content: 'Python 装饰器怎么用？给我一个带参数的例子', status: 'COMPLETED', createdAt: ago(1500),
    },
    {
      id: 'msg-3-2', sessionId: 'sess-3', role: 'assistant',
      content: '装饰器是 Python 中非常强大的特性，下面是一个带参数的装饰器示例：\n\n```python\ndef retry(max_attempts=3, delay=1):\n    """重试装饰器：函数执行失败时自动重试"""\n    def decorator(func):\n        from functools import wraps\n        import time\n        \n        @wraps(func)\n        def wrapper(*args, **kwargs):\n            for attempt in range(max_attempts):\n                try:\n                    return func(*args, **kwargs)\n                except Exception as e:\n                    if attempt == max_attempts - 1:\n                        raise\n                    print(f"重试 {attempt+1}/{max_attempts}... 错误: {e}")\n                    time.sleep(delay)\n        return wrapper\n    return decorator\n\n@retry(max_attempts=3, delay=2)\ndef fetch_data(url):\n    pass\n```\n\n关键点：装饰器工厂函数（`retry`）接收参数，返回真正的装饰器 `decorator`。',
      status: 'COMPLETED', agentId: 'agent-1', createdAt: ago(1490),
    },
  ],
}

// ── Helpers ──

export function getSessionDetail(sessionId: string): ChatSessionDetailVO {
  const session = sessions.find((s) => s.id === sessionId)
  if (!session) throw new Error('Session not found')
  return { ...session, messages: sessionMessages[sessionId] ?? [] }
}

export function agentToSummary(a: AgentDetailVO): AgentSummaryVO {
  const { systemPrompt, maxSteps, modelId, version, toolBindings, skillIds, knowledgeBaseIds, ...summary } = a
  return summary as AgentSummaryVO
}
