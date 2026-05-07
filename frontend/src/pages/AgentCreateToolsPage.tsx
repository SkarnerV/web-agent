import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ChevronLeft, Check, Search, Wand, Plug, BookOpen, Plus, X,
  Globe, FileText, Code, Database, Mail, Calendar, Wrench, Trash2, Loader2,
  HelpCircle, ListTodo,
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { listSkills } from '../api/skill'
import { listMcps } from '../api/mcp'
import { listKnowledgeBases } from '../api/knowledge'
import type { SkillSummaryVO, McpSummaryVO, KnowledgeBaseSummaryVO } from '../api/types'
import {
  readAgentWizardDraft,
  saveAgentWizardDraft,
  type AgentWizardToolDraft,
  type AgentWizardToolType,
} from './agentWizardDraft'

type StepType = 1 | 2 | 3 | 4

// ── Built-in tool primitives ──

const builtinTools = [
  { id: 'web_search', name: 'web_search', description: '搜索互联网获取最新信息', icon: Globe },
  { id: 'question', name: 'question', description: '向用户提问并等待用户选择后继续执行', icon: HelpCircle },
  { id: 'todo', name: 'todo', description: '创建或更新右侧信息栏中的任务清单', icon: ListTodo },
  { id: 'read_file', name: 'read_file', description: '读取文件内容', icon: FileText },
  { id: 'write_file', name: 'write_file', description: '创建或更新文件', icon: FileText },
  { id: 'execute_code', name: 'execute_code', description: '在沙箱环境中执行代码', icon: Code },
  { id: 'database_query', name: 'database_query', description: '查询数据库获取结构化数据', icon: Database },
  { id: 'send_email', name: 'send_email', description: '发送电子邮件', icon: Mail },
  { id: 'calendar', name: 'calendar', description: '管理日程和提醒', icon: Calendar },
]

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// ── Types ──

interface ToolItem {
  id: string
  name: string
  description?: string
  type: 'builtin' | 'skill' | 'mcp' | 'kb'
  tools?: string[]
  icon: React.FC<{ className?: string }>
  docs?: number
  size?: string
  indexedAt?: string
}

const iconForToolType = (type: AgentWizardToolType, id?: string) => {
  if (type === 'skill') return Wand
  if (type === 'mcp') return Plug
  if (type === 'kb') return BookOpen
  return builtinTools.find((tool) => tool.id === id)?.icon ?? Wrench
}

const fromDraftTool = (tool: AgentWizardToolDraft): ToolItem => ({
  ...tool,
  icon: iconForToolType(tool.type, tool.id),
})

const toDraftTool = (tool: ToolItem): AgentWizardToolDraft => ({
  id: tool.id,
  name: tool.name,
  description: tool.description,
  type: tool.type,
})

const StepsColumn: React.FC<{ activeStep: StepType }> = ({ activeStep }) => {
  const steps = [
    { step: 1, label: '基本信息' },
    { step: 2, label: '工具配置' },
    { step: 3, label: '协作智能体' },
    { step: 4, label: '调试发布' },
  ]
  return (
    <div className="w-[240px] p-6 bg-white border border-border-subtle rounded-lg flex flex-col gap-2 flex-shrink-0">
      <span className="text-[11px] font-semibold text-text-tertiary">配置步骤</span>
      {steps.map((s) => (
        <div
          key={s.step}
          className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg transition-colors ${
            activeStep === s.step ? 'bg-brand-50' : 'bg-transparent'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 ${
              activeStep === s.step ? 'bg-brand-500 text-white'
              : s.step < activeStep ? 'bg-success-500 text-white'
              : 'bg-gray-100 text-text-tertiary'
            }`}
          >
            {activeStep === s.step || s.step < activeStep ? (
              <Check className="w-3 h-3" />
            ) : (
              <span className="text-xs font-semibold">{s.step}</span>
            )}
          </div>
          <span className={`text-[13px] ${activeStep === s.step ? 'font-semibold text-brand-500' : 'text-text-secondary'}`}>
            {s.label}
          </span>
        </div>
      ))}
      <div className="mt-2 px-3 py-4 flex flex-col gap-1">
        <span className="text-[11px] text-text-tertiary">自动保存</span>
        <span className="text-[11px] font-medium text-success-500">刚刚</span>
      </div>
    </div>
  )
}

// ── Modal ──

const AddModal: React.FC<{
  open: boolean
  onClose: () => void
  onAdd: (item: ToolItem) => void
  existingIds: string[]
}> = ({ open, onClose, onAdd, existingIds }) => {
  const [search, setSearch] = useState('')
  const [tab, setTab] = useState<'builtin' | 'skill' | 'mcp' | 'kb'>('builtin')
  const [skillItems, setSkillItems] = useState<ToolItem[]>([])
  const [mcpItems, setMcpItems] = useState<ToolItem[]>([])
  const [kbItems, setKbItems] = useState<ToolItem[]>([])
  const [loading, setLoading] = useState<Record<string, boolean>>({})

  const fetchTab = useCallback(async (key: 'skill' | 'mcp' | 'kb') => {
    setLoading((prev) => ({ ...prev, [key]: true }))
    try {
      if (key === 'skill') {
        const res = await listSkills({ page: 1, page_size: 100 })
        setSkillItems(res.data.map((s: SkillSummaryVO) => ({
          id: s.id,
          name: s.name,
          description: s.description,
          type: 'skill' as const,
          icon: Wand,
        })))
      } else if (key === 'mcp') {
        const res = await listMcps({ page: 1, page_size: 100 })
        setMcpItems(res.data.map((m: McpSummaryVO) => ({
          id: m.id,
          name: m.name,
          description: m.toolsDiscoveredCount != null ? `${m.toolsDiscoveredCount} 个工具` : undefined,
          type: 'mcp' as const,
          icon: Plug,
        })))
      } else {
        const res = await listKnowledgeBases({ page: 1, page_size: 100 })
        setKbItems(res.data.map((k: KnowledgeBaseSummaryVO) => ({
          id: k.id,
          name: k.name,
          description: `${k.docCount} 文档 · ${formatBytes(k.totalSizeBytes)}`,
          type: 'kb' as const,
          icon: BookOpen,
          docs: k.docCount,
          size: formatBytes(k.totalSizeBytes),
        })))
      }
    } catch {
      // silently ignore
    } finally {
      setLoading((prev) => ({ ...prev, [key]: false }))
    }
  }, [])

  useEffect(() => {
    if (!open) return
    if (tab !== 'builtin') fetchTab(tab)
  }, [open, tab, fetchTab])

  const tabs = [
    { key: 'builtin' as const, label: '内置工具', icon: Wrench },
    { key: 'skill' as const, label: 'Skill', icon: Wand },
    { key: 'mcp' as const, label: 'MCP', icon: Plug },
    { key: 'kb' as const, label: '知识库', icon: BookOpen },
  ]

  const lc = search.toLowerCase()
  const builtinFiltered = builtinTools.filter(
    (t) => t.name.toLowerCase().includes(lc) && !existingIds.includes(t.id),
  )
  const skillsFiltered = skillItems.filter(
    (s) => s.name.toLowerCase().includes(lc) && !existingIds.includes(s.id),
  )
  const mcpsFiltered = mcpItems.filter(
    (m) => m.name.toLowerCase().includes(lc) && !existingIds.includes(m.id),
  )
  const kbsFiltered = kbItems.filter(
    (k) => k.name.toLowerCase().includes(lc) && !existingIds.includes(k.id),
  )

  if (!open) return null

  const renderLoading = () => (
    <div className="flex items-center justify-center py-8">
      <Loader2 className="w-5 h-5 text-text-tertiary animate-spin" />
    </div>
  )

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-xl w-[640px] max-h-[600px] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-border-subtle">
          <h3 className="text-[15px] font-semibold text-text-primary">添加工具</h3>
          <button onClick={onClose} className="w-8 h-8 rounded-md flex items-center justify-center text-text-tertiary hover:bg-gray-100">
            <X className="w-4 h-4" />
          </button>
        </div>
        {/* Search */}
        <div className="px-5 pt-4">
          <div className="flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-md">
            <Search className="w-3.5 h-3.5 text-text-tertiary" />
            <input
              type="text"
              placeholder="搜索工具、Skill、MCP、知识库..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 bg-transparent text-[13px] outline-none placeholder:text-text-tertiary"
            />
          </div>
        </div>
        {/* Tabs */}
        <div className="flex items-center gap-1 px-5 pt-3 pb-3 border-b border-border-subtle">
          {tabs.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-[13px] transition-colors ${
                tab === t.key ? 'bg-brand-50 text-brand-500 font-semibold' : 'text-text-secondary hover:bg-gray-50'
              }`}
            >
              <t.icon className="w-3.5 h-3.5" />
              {t.label}
            </button>
          ))}
        </div>
        {/* Content */}
        <div className="flex-1 overflow-auto p-5 flex flex-col gap-3">
          {tab === 'builtin' && builtinFiltered.map((item) => (
            <button
              key={item.id}
              onClick={() => onAdd({ ...item, type: 'builtin' })}
              className="flex items-center gap-3 p-3 rounded-lg border border-border-subtle hover:border-brand-500 text-left transition-colors"
            >
              <div className="w-9 h-9 rounded-lg bg-brand-50 flex items-center justify-center flex-shrink-0">
                <item.icon className="w-4.5 h-4.5 text-brand-500" />
              </div>
              <div className="flex-1 flex flex-col gap-0.5">
                <span className="text-[13px] font-semibold text-text-primary">{item.name}</span>
                <span className="text-[11px] text-text-tertiary">{item.description}</span>
              </div>
              <Plus className="w-4 h-4 text-brand-500" />
            </button>
          ))}
          {tab === 'skill' && (loading.skill ? renderLoading() : skillsFiltered.length === 0 ? (
            <p className="text-center text-[13px] text-text-tertiary py-8">所有 Skill 已添加</p>
          ) : (
            skillsFiltered.map((item) => (
              <button
                key={item.id}
                onClick={() => onAdd(item)}
                className="flex items-center gap-3 p-3 rounded-lg border border-border-subtle hover:border-brand-500 text-left transition-colors"
              >
                <div className="w-9 h-9 rounded-lg bg-purple-50 flex items-center justify-center flex-shrink-0">
                  <Wand className="w-4.5 h-4.5 text-purple-500" />
                </div>
                <div className="flex-1 flex flex-col gap-0.5">
                  <span className="text-[13px] font-semibold text-text-primary">{item.name}</span>
                  <span className="text-[11px] text-text-tertiary">{item.description}</span>
                </div>
                <Plus className="w-4 h-4 text-brand-500" />
              </button>
            ))
          ))}
          {tab === 'mcp' && (loading.mcp ? renderLoading() : mcpsFiltered.length === 0 ? (
            <p className="text-center text-[13px] text-text-tertiary py-8">所有 MCP 已添加</p>
          ) : (
            mcpsFiltered.map((item) => (
              <button
                key={item.id}
                onClick={() => onAdd(item)}
                className="flex items-center gap-3 p-3 rounded-lg border border-border-subtle hover:border-brand-500 text-left transition-colors"
              >
                <div className="w-9 h-9 rounded-lg bg-success-50 flex items-center justify-center flex-shrink-0">
                  <Plug className="w-4.5 h-4.5 text-success-500" />
                </div>
                <div className="flex-1 flex flex-col gap-0.5">
                  <span className="text-[13px] font-semibold text-text-primary">{item.name}</span>
                  <span className="text-[11px] text-text-tertiary">{item.description}</span>
                </div>
                <Plus className="w-4 h-4 text-brand-500" />
              </button>
            ))
          ))}
          {tab === 'kb' && (loading.kb ? renderLoading() : kbsFiltered.length === 0 ? (
            <p className="text-center text-[13px] text-text-tertiary py-8">所有知识库已引用</p>
          ) : (
            kbsFiltered.map((item) => (
              <button
                key={item.id}
                onClick={() => onAdd(item)}
                className="flex items-center gap-3 p-3 rounded-lg border border-border-subtle hover:border-brand-500 text-left transition-colors"
              >
                <div className="w-9 h-9 rounded-lg bg-warning-50 flex items-center justify-center flex-shrink-0">
                  <BookOpen className="w-4.5 h-4.5 text-warning-500" />
                </div>
                <div className="flex-1 flex flex-col gap-0.5">
                  <span className="text-[13px] font-semibold text-text-primary">{item.name}</span>
                  <span className="text-[11px] text-text-tertiary">{item.description}</span>
                </div>
                <Plus className="w-4 h-4 text-brand-500" />
              </button>
            ))
          ))}
        </div>
      </div>
    </div>
  )
}

// ── Page Component ──

const AgentCreateToolsPage: React.FC = () => {
  const navigate = useNavigate()
  const [addedTools, setAddedTools] = useState<ToolItem[]>(() =>
    readAgentWizardDraft().tools.map(fromDraftTool),
  )
  const [showModal, setShowModal] = useState(false)

  useEffect(() => {
    saveAgentWizardDraft({ tools: addedTools.map(toDraftTool) })
  }, [addedTools])

  const handleAdd = (item: ToolItem) => {
    setAddedTools((prev) => [...prev, item])
    setShowModal(false)
  }

  const handleRemove = (id: string) => {
    setAddedTools((prev) => prev.filter((t) => t.id !== id))
  }

  const builtins = addedTools.filter((t) => t.type === 'builtin')
  const skills = addedTools.filter((t) => t.type === 'skill')
  const mcps = addedTools.filter((t) => t.type === 'mcp')
  const kbs = addedTools.filter((t) => t.type === 'kb')

  const renderSection = (
    label: string,
    icon: React.FC<{ className?: string }>,
    items: ToolItem[],
    iconBg: string,
    iconColor: string,
  ) => (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        {React.createElement(icon, { className: 'w-4 h-4 text-text-secondary' })}
        <span className="text-[13px] font-semibold text-text-primary">{label}</span>
        {items.length > 0 && (
          <span className="text-[11px] text-text-tertiary">{items.length} 个已添加</span>
        )}
      </div>
      {items.length === 0 ? (
        <p className="text-[13px] text-text-tertiary py-3 px-4 bg-gray-50 rounded-lg">
          暂无{label}，点击上方"添加工具"按钮添加
        </p>
      ) : (
        <div className="flex flex-col gap-2">
          {items.map((item) => (
            <div
              key={item.id}
              className="flex items-center gap-3 p-3 bg-white border border-border-subtle rounded-lg hover:border-border-strong transition-colors group"
            >
              <div className={`w-9 h-9 rounded-lg ${iconBg} flex items-center justify-center flex-shrink-0`}>
                <item.icon className={`w-4.5 h-4.5 ${iconColor}`} />
              </div>
              <div className="flex-1 flex flex-col gap-0.5 min-w-0">
                <span className="text-[13px] font-semibold text-text-primary">{item.name}</span>
                {item.type === 'mcp' && item.tools ? (
                  <span className="text-[11px] text-text-tertiary truncate">
                    添加了 {item.tools.length} 个工具：{item.tools.join(' · ')}
                  </span>
                ) : item.type === 'kb' && item.docs ? (
                  <span className="text-[11px] text-text-tertiary">
                    {item.docs} 文档 · {item.size} · 上次索引 {item.indexedAt}
                  </span>
                ) : (
                  <span className="text-[11px] text-text-tertiary">{item.description}</span>
                )}
              </div>
              <button
                onClick={() => handleRemove(item.id)}
                className="w-7 h-7 rounded-md flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 opacity-0 group-hover:opacity-100 transition-all flex-shrink-0"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )

  const sectionConfigs = [
    { label: '内置工具', icon: Wrench, items: builtins, bg: 'bg-brand-50', color: 'text-brand-500' },
    { label: 'Skill', icon: Wand, items: skills, bg: 'bg-purple-50', color: 'text-purple-500' },
    { label: 'MCP 服务', icon: Plug, items: mcps, bg: 'bg-success-50', color: 'text-success-500' },
    { label: '知识库', icon: BookOpen, items: kbs, bg: 'bg-warning-50', color: 'text-warning-500' },
  ]

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '创建' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center gap-3 px-6">
        <button
          onClick={() => navigate('/agents/create')}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回</span>
        </button>
        <span className="text-[15px] font-semibold text-text-primary flex-1">创建智能体</span>
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={() => navigate('/agents/create')}>保存草稿</Button>
          <Button variant="primary">发布</Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        <StepsColumn activeStep={2} />

        <div className="flex-1 overflow-auto">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5">
            {/* Header */}
            <div className="flex flex-col gap-1">
              <h2 className="text-lg font-bold text-text-primary">工具配置</h2>
              <p className="text-[13px] text-text-tertiary">
                为智能体添加内置工具、Skill、MCP 服务、知识库引用
              </p>
            </div>

            {/* Add Button */}
            <div>
              <Button variant="secondary" icon={<Plus className="w-4 h-4" />} onClick={() => setShowModal(true)}>
                添加工具
              </Button>
            </div>

            {/* Sections */}
            {sectionConfigs.map((cfg, i) => (
              <React.Fragment key={cfg.label}>
                {renderSection(cfg.label, cfg.icon, cfg.items, cfg.bg, cfg.color)}
                {i < sectionConfigs.length - 1 && <div className="h-px bg-border-subtle" />}
              </React.Fragment>
            ))}

            {/* Bottom Row */}
            <div className="flex items-center justify-end gap-2 pt-4">
              <Button variant="secondary" onClick={() => navigate('/agents/create')}>上一步</Button>
              <Button variant="primary" onClick={() => navigate('/agents/collab')}>下一步</Button>
            </div>
          </div>
        </div>
      </div>

      {/* Add Modal */}
      <AddModal
        open={showModal}
        onClose={() => setShowModal(false)}
        onAdd={handleAdd}
        existingIds={addedTools.map((t) => t.id)}
      />
    </Layout>
  )
}

export default AgentCreateToolsPage
