import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  BookOpen,
  Bot,
  Calendar,
  ChevronLeft,
  Code,
  Database,
  FileText,
  Globe,
  Mail,
  MessageSquare,
  Plug,
  Plus,
  Sparkles,
  Trash2,
  Users,
  Wand,
  Wrench,
  X,
  Zap,
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { getAgent, updateAgent, deleteAgent, listAgents } from '../api/agent'
import { publishAsset } from '../api/market'
import { listAllModels } from '../api/model'
import { listSkills } from '../api/skill'
import { listMcps } from '../api/mcp'
import { listKnowledgeBases } from '../api/knowledge'
import { ApiError } from '../api/client'
import type {
  AgentSummaryVO,
  AgentUpdateRequest,
  KnowledgeBaseSummaryVO,
  McpSummaryVO,
  ModelInfo,
  SkillSummaryVO,
  ToolBindingRequest,
  ToolBindingVO,
} from '../api/types'

type BindingType = 'builtin' | 'skill' | 'mcp' | 'kb' | 'collaborator'

interface BindingItem {
  id: string
  name: string
  description?: string
  type: BindingType
  sourceId?: string
  toolName?: string
}

const iconOptions = [
  { id: 'bot', icon: Bot, bg: 'bg-brand-50', color: 'text-brand-500' },
  { id: 'sparkles', icon: Sparkles, bg: 'bg-warning-50', color: 'text-warning-500' },
  { id: 'zap', icon: Zap, bg: 'bg-success-50', color: 'text-success-500' },
  { id: 'message', icon: MessageSquare, bg: 'bg-error-50', color: 'text-error-500' },
]

const builtinTools: BindingItem[] = [
  { id: 'web_search', name: 'web_search', description: '搜索互联网获取最新信息', type: 'builtin' },
  { id: 'ask_question', name: 'ask_question', description: '向用户提问以获取更多信息', type: 'builtin' },
  { id: 'read_file', name: 'read_file', description: '读取文件内容', type: 'builtin' },
  { id: 'write_file', name: 'write_file', description: '创建或更新文件', type: 'builtin' },
  { id: 'execute_code', name: 'execute_code', description: '在沙箱环境中执行代码', type: 'builtin' },
  { id: 'database_query', name: 'database_query', description: '查询数据库获取结构化数据', type: 'builtin' },
  { id: 'send_email', name: 'send_email', description: '发送电子邮件', type: 'builtin' },
  { id: 'calendar', name: 'calendar', description: '管理日程和提醒', type: 'builtin' },
]

const emptySelects: Record<BindingType, string> = {
  builtin: '',
  skill: '',
  mcp: '',
  kb: '',
  collaborator: '',
}

const bindingIcon: Record<BindingType, React.FC<{ className?: string }>> = {
  builtin: Wrench,
  skill: Wand,
  mcp: Plug,
  kb: BookOpen,
  collaborator: Users,
}

const builtinIcon: Record<string, React.FC<{ className?: string }>> = {
  web_search: Globe,
  ask_question: Wand,
  read_file: FileText,
  write_file: FileText,
  execute_code: Code,
  database_query: Database,
  send_email: Mail,
  calendar: Calendar,
}

const iconStyle: Record<BindingType, { bg: string; color: string }> = {
  builtin: { bg: 'bg-brand-50', color: 'text-brand-500' },
  skill: { bg: 'bg-purple-50', color: 'text-purple-500' },
  mcp: { bg: 'bg-success-50', color: 'text-success-500' },
  kb: { bg: 'bg-warning-50', color: 'text-warning-500' },
  collaborator: { bg: 'bg-gray-100', color: 'text-gray-600' },
}

function formatBytes(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

function catalogFallback(id: string, type: BindingType): BindingItem {
  return { id, name: id, type }
}

function selectedToolBinding(binding: ToolBindingVO, mcpCatalog: BindingItem[]): BindingItem | null {
  if (binding.sourceType === 'builtin') {
    const builtin = builtinTools.find((tool) => tool.id === binding.toolName)
    return {
      ...(builtin ?? catalogFallback(binding.toolName, 'builtin')),
      toolName: binding.toolName,
    }
  }

  if (binding.sourceType === 'mcp') {
    const match = binding.sourceId
      ? mcpCatalog.find((item) => item.id === binding.sourceId)
      : undefined
    return {
      id: binding.sourceId || binding.id,
      name: match?.name ?? binding.toolName,
      description: match?.description,
      type: 'mcp',
      sourceId: binding.sourceId,
      toolName: binding.toolName,
    }
  }

  return null
}

const PublishDialog: React.FC<{
  open: boolean
  publishing: boolean
  onClose: () => void
  onConfirm: (data: { visibility: string; version: string; releaseNotes: string }) => void
}> = ({ open, publishing, onClose, onConfirm }) => {
  const [visibility, setVisibility] = useState('public')
  const [version, setVersion] = useState('v1.0.0')
  const [releaseNotes, setReleaseNotes] = useState('')

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-[460px] bg-white rounded-xl shadow-xl p-6 flex flex-col gap-5">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-text-primary">发布智能体</h3>
          <button onClick={onClose} className="text-text-tertiary hover:text-text-primary">
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-[13px] font-medium text-text-primary">可见性</label>
          <div className="flex flex-col gap-1.5">
            {([
              { value: 'public', label: '公开' },
              { value: 'group_edit', label: '同组可编辑' },
              { value: 'group_read', label: '同组只读' },
              { value: 'private', label: '私有' },
            ] as const).map((opt) => (
              <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="visibility"
                  value={opt.value}
                  checked={visibility === opt.value}
                  onChange={() => setVisibility(opt.value)}
                  className="accent-brand-500"
                />
                <span className="text-sm text-text-primary">{opt.label}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-[13px] font-medium text-text-primary">版本号</label>
          <input
            type="text"
            value={version}
            onChange={(e) => setVersion(e.target.value)}
            className="w-full px-3 py-2 bg-white border border-border-strong rounded-md text-sm text-text-primary outline-none focus:border-brand-500"
          />
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-[13px] font-medium text-text-primary">发布说明</label>
          <textarea
            value={releaseNotes}
            onChange={(e) => setReleaseNotes(e.target.value)}
            placeholder="描述本次发布的变更..."
            className="w-full px-3 py-2 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-[80px] focus:border-brand-500"
          />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" onClick={onClose} disabled={publishing}>
            取消
          </Button>
          <Button variant="primary" onClick={() => onConfirm({ visibility, version, releaseNotes })} disabled={publishing}>
            {publishing ? '发布中...' : '确认发布'}
          </Button>
        </div>
      </div>
    </div>
  )
}

const ConfigSection: React.FC<{
  title: string
  description: string
  type: BindingType
  items: BindingItem[]
  options: BindingItem[]
  selectedId: string
  onSelectedChange: (id: string) => void
  onAdd: () => void
  onRemove: (item: BindingItem) => void
}> = ({
  title,
  description,
  type,
  items,
  options,
  selectedId,
  onSelectedChange,
  onAdd,
  onRemove,
}) => {
  const SectionIcon = bindingIcon[type]
  const remainingOptions = options.filter(
    (option) => !items.some((item) => item.id === option.id),
  )
  const style = iconStyle[type]

  return (
    <section className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <SectionIcon className="w-4 h-4 text-text-secondary" />
        <div className="flex flex-col gap-0.5">
          <h3 className="text-sm font-semibold text-text-primary">{title}</h3>
          <p className="text-xs text-text-tertiary">{description}</p>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        {items.length === 0 && (
          <p className="px-4 py-3 bg-gray-50 rounded-lg text-[13px] text-text-tertiary">
            暂无已配置项
          </p>
        )}
        {items.map((item) => {
          const ItemIcon = type === 'builtin' ? (builtinIcon[item.id] ?? Wrench) : SectionIcon
          return (
            <div
              key={`${item.type}:${item.id}`}
              className="flex items-center gap-3 p-3 bg-white border border-border-subtle rounded-lg group"
            >
              <div className={`w-9 h-9 rounded-lg ${style.bg} flex items-center justify-center flex-shrink-0`}>
                <ItemIcon className={`w-4.5 h-4.5 ${style.color}`} />
              </div>
              <div className="min-w-0 flex-1 flex flex-col gap-0.5">
                <span className="text-[13px] font-semibold text-text-primary truncate">{item.name}</span>
                {item.description && (
                  <span className="text-[11px] text-text-tertiary truncate">{item.description}</span>
                )}
              </div>
              <button
                onClick={() => onRemove(item)}
                className="w-7 h-7 rounded-md flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 opacity-0 group-hover:opacity-100 transition-all"
                aria-label={`删除${item.name}`}
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          )
        })}
      </div>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <select
          value={selectedId}
          onChange={(e) => onSelectedChange(e.target.value)}
          className="flex-1 px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary outline-none focus:border-brand-500"
        >
          <option value="">选择要添加的项</option>
          {remainingOptions.map((option) => (
            <option key={option.id} value={option.id}>
              {option.name}
            </option>
          ))}
        </select>
        <Button
          variant="secondary"
          icon={<Plus className="w-4 h-4" />}
          onClick={onAdd}
          disabled={!selectedId}
        >
          添加
        </Button>
      </div>
    </section>
  )
}

const AgentEditPage: React.FC = () => {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [models, setModels] = useState<ModelInfo[]>([])
  const [catalog, setCatalog] = useState<Record<BindingType, BindingItem[]>>({
    builtin: builtinTools,
    skill: [],
    mcp: [],
    kb: [],
    collaborator: [],
  })
  const [bindings, setBindings] = useState<BindingItem[]>([])
  const [selectedToAdd, setSelectedToAdd] = useState<Record<BindingType, string>>(emptySelects)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    icon: 'bot',
    model: '',
    maxSteps: 10,
    prompt: '',
    version: 0,
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showPublishDialog, setShowPublishDialog] = useState(false)
  const [publishing, setPublishing] = useState(false)

  useEffect(() => {
    if (!id) return
    ;(async () => {
      try {
        const [agent, allModels, skillsRes, mcpsRes, kbsRes, agentsRes] = await Promise.all([
          getAgent(id),
          listAllModels(),
          listSkills({ page: 1, page_size: 100 }).catch(() => ({ data: [] as SkillSummaryVO[] })),
          listMcps({ page: 1, page_size: 100 }).catch(() => ({ data: [] as McpSummaryVO[] })),
          listKnowledgeBases({ page: 1, page_size: 100 }).catch(() => ({ data: [] as KnowledgeBaseSummaryVO[] })),
          listAgents({ page: 1, page_size: 100 }).catch(() => ({ data: [] as AgentSummaryVO[] })),
        ])

        const skillCatalog = skillsRes.data.map((skill) => ({
          id: skill.id,
          name: skill.name,
          description: skill.description,
          type: 'skill' as const,
        }))
        const mcpCatalog = mcpsRes.data.map((mcp) => ({
          id: mcp.id,
          name: mcp.name,
          description: mcp.toolsDiscoveredCount != null ? `${mcp.toolsDiscoveredCount} 个工具` : mcp.url,
          type: 'mcp' as const,
          sourceId: mcp.id,
        }))
        const kbCatalog = kbsRes.data.map((kb) => ({
          id: kb.id,
          name: kb.name,
          description: `${kb.docCount} 文档 · ${formatBytes(kb.totalSizeBytes)}`,
          type: 'kb' as const,
        }))
        const collaboratorCatalog = agentsRes.data
          .filter((agentItem) => agentItem.id !== id)
          .map((agentItem) => ({
            id: agentItem.id,
            name: agentItem.name,
            description: agentItem.description,
            type: 'collaborator' as const,
          }))

        setCatalog({
          builtin: builtinTools,
          skill: skillCatalog,
          mcp: mcpCatalog,
          kb: kbCatalog,
          collaborator: collaboratorCatalog,
        })

        const enabled = allModels.filter((m) => m.enabled)
        setModels(enabled)
        const defaultModel = enabled.find((m) => m.isDefault) ?? enabled[0]
        setFormData({
          name: agent.name,
          description: agent.description ?? '',
          icon: agent.avatar ?? 'bot',
          model: agent.modelId ?? defaultModel?.id ?? '',
          maxSteps: agent.maxSteps || 10,
          prompt: agent.systemPrompt ?? '',
          version: agent.version,
        })

        const selectedBindings: BindingItem[] = [
          ...(agent.toolBindings ?? [])
            .map((binding) => selectedToolBinding(binding, mcpCatalog))
            .filter((binding): binding is BindingItem => binding !== null),
          ...(agent.skillIds ?? []).map((skillId) =>
            skillCatalog.find((skill) => skill.id === skillId) ?? catalogFallback(skillId, 'skill'),
          ),
          ...(agent.knowledgeBaseIds ?? []).map((kbId) =>
            kbCatalog.find((kb) => kb.id === kbId) ?? catalogFallback(kbId, 'kb'),
          ),
          ...((agent.collaboratorAgentIds ?? []) as string[]).map((agentId) =>
            collaboratorCatalog.find((collaborator) => collaborator.id === agentId) ??
            catalogFallback(agentId, 'collaborator'),
          ),
        ]
        setBindings(selectedBindings)
      } catch (e) {
        setError(e instanceof ApiError ? e.message : 'Failed to load agent')
      } finally {
        setLoading(false)
      }
    })()
  }, [id])

  const itemsOfType = (type: BindingType) => bindings.filter((item) => item.type === type)

  const handleAddBinding = (type: BindingType) => {
    const selectedId = selectedToAdd[type]
    if (!selectedId) return
    const item = catalog[type].find((option) => option.id === selectedId)
    if (!item) return

    setBindings((prev) =>
      prev.some((existing) => existing.type === type && existing.id === item.id)
        ? prev
        : [...prev, item],
    )
    setSelectedToAdd((prev) => ({ ...prev, [type]: '' }))
  }

  const handleRemoveBinding = (item: BindingItem) => {
    setBindings((prev) =>
      prev.filter((existing) => !(existing.type === item.type && existing.id === item.id)),
    )
  }

  const buildToolBindings = (): ToolBindingRequest[] =>
    bindings
      .filter((item) => item.type === 'builtin' || item.type === 'mcp')
      .map((item) => {
        if (item.type === 'builtin') {
          return {
            sourceType: 'builtin',
            toolName: item.toolName ?? item.name,
            enabled: true,
          }
        }
        return {
          sourceType: 'mcp',
          sourceId: item.sourceId,
          toolName: item.toolName ?? item.name,
          enabled: true,
        }
      })

  const buildUpdateRequest = (): AgentUpdateRequest => ({
    name: formData.name.trim(),
    description: formData.description.trim() || undefined,
    avatar: formData.icon,
    modelId: formData.model || undefined,
    systemPrompt: formData.prompt.trim() || undefined,
    maxSteps: formData.maxSteps,
    version: formData.version,
    toolBindings: buildToolBindings(),
    skillIds: itemsOfType('skill').map((item) => item.id),
    knowledgeBaseIds: itemsOfType('kb').map((item) => item.id),
    collaboratorAgentIds: itemsOfType('collaborator').map((item) => item.id),
  })

  const handleSave = async () => {
    if (!id || !formData.name.trim()) return
    setSaving(true)
    setError(null)
    try {
      await updateAgent(id, buildUpdateRequest())
      navigate('/agents')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '保存失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  const handlePublish = async (data: { visibility: string; version: string; releaseNotes: string }) => {
    if (!id || !formData.name.trim()) return
    setPublishing(true)
    setError(null)
    try {
      await updateAgent(id, buildUpdateRequest())
      await publishAsset({
        assetType: 'AGENT',
        assetId: id,
        visibility: data.visibility,
        version: data.version,
        releaseNotes: data.releaseNotes || undefined,
      })
      navigate('/agents', { state: { published: true } })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '发布失败，请重试')
    } finally {
      setPublishing(false)
      setShowPublishDialog(false)
    }
  }

  const handleDelete = async () => {
    if (!id || !formData.name) return
    if (!window.confirm(`确定要删除 "${formData.name}" 吗？此操作不可撤销。`)) return
    try {
      await deleteAgent(id)
      navigate('/agents')
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        if (window.confirm('该智能体被其他资源引用，是否强制删除？')) {
          try {
            await deleteAgent(id, true)
            navigate('/agents')
          } catch (e2) {
            alert(e2 instanceof Error ? e2.message : '删除失败')
          }
        }
      } else {
        alert(e instanceof Error ? e.message : '删除失败')
      }
    }
  }

  if (loading) {
    return (
      <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '编辑' }]}>
        <div className="flex items-center justify-center h-64">
          <div className="w-6 h-6 border-2 border-brand-500 border-t-transparent rounded-full animate-spin" />
        </div>
      </Layout>
    )
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '编辑' }]}>
      <div className="h-14 bg-white border-b border-border-subtle flex items-center gap-3 px-6">
        <button
          onClick={() => navigate('/agents')}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回</span>
        </button>
        <span className="text-[15px] font-semibold text-text-primary flex-1">
          编辑智能体：{formData.name}
        </span>
        <div className="flex items-center gap-2">
          <button
            onClick={handleDelete}
            className="px-3 py-1.5 rounded-md text-[13px] font-medium text-error-500 hover:bg-error-50 transition-colors"
          >
            删除
          </button>
          <Button
            variant="primary"
            onClick={() => setShowPublishDialog(true)}
            disabled={saving || publishing || !formData.name.trim()}
          >
            发布
          </Button>
        </div>
      </div>

      <div className="flex-1 p-8 overflow-auto">
        <div className="max-w-[1040px]">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-6">
            <div className="flex flex-col gap-1">
              <h2 className="text-lg font-bold text-text-primary">基本信息</h2>
              <p className="text-[13px] text-text-tertiary">设置智能体的基础身份、工具能力和协作关系</p>
            </div>

            {error && (
              <div className="px-3 py-2 bg-error-50 border border-error-200 rounded text-sm text-error-500">
                {error}
              </div>
            )}

            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">头像</label>
              <div className="flex items-center gap-3">
                {iconOptions.map((opt) => {
                  const IconComponent = opt.icon
                  return (
                    <button
                      key={opt.id}
                      onClick={() => setFormData({ ...formData, icon: opt.id })}
                      className={`w-10 h-10 rounded-lg flex items-center justify-center transition-all ${
                        formData.icon === opt.id
                          ? `${opt.bg} border-2 border-brand-500`
                          : 'bg-gray-100 border border-border-subtle hover:border-border-strong'
                      }`}
                    >
                      <IconComponent
                        className={`w-5 h-5 ${formData.icon === opt.id ? opt.color : 'text-gray-600'}`}
                      />
                    </button>
                  )
                })}
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-1">
                <label className="text-[13px] font-medium text-text-primary">名称</label>
                <span className="text-error-500">*</span>
              </div>
              <Input
                placeholder="输入智能体名称"
                value={formData.name}
                onChange={(value) => setFormData({ ...formData, name: value })}
              />
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">描述</label>
              <textarea
                placeholder="描述智能体的功能和使用场景"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-[72px] focus:border-brand-500"
              />
            </div>

            <div className="flex gap-4">
              <div className="flex-1 flex flex-col gap-2">
                <label className="text-[13px] font-medium text-text-primary">模型</label>
                <select
                  value={formData.model}
                  onChange={(e) => setFormData({ ...formData, model: e.target.value })}
                  className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary outline-none focus:border-brand-500"
                >
                  {models.map((m) => (
                    <option key={m.id} value={m.id}>{m.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex-1 flex flex-col gap-2">
                <label className="text-[13px] font-medium text-text-primary">最大步骤数</label>
                <input
                  type="number"
                  min={1}
                  max={50}
                  value={formData.maxSteps}
                  onChange={(e) => setFormData({ ...formData, maxSteps: parseInt(e.target.value) || 1 })}
                  className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary outline-none focus:border-brand-500"
                />
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <label className="text-[13px] font-medium text-text-primary">提示词</label>
                <span className="text-[11px] text-text-tertiary">{formData.prompt.length}/2000</span>
              </div>
              <textarea
                placeholder="系统提示词..."
                value={formData.prompt}
                onChange={(e) => setFormData({ ...formData, prompt: e.target.value })}
                className="w-full px-3.5 py-3 bg-gray-50 border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-[160px] focus:border-brand-500"
              />
            </div>

            <div className="h-px bg-border-subtle" />

            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <ConfigSection
                title="内置工具"
                description="平台提供的通用工具能力"
                type="builtin"
                items={itemsOfType('builtin')}
                options={catalog.builtin}
                selectedId={selectedToAdd.builtin}
                onSelectedChange={(value) => setSelectedToAdd((prev) => ({ ...prev, builtin: value }))}
                onAdd={() => handleAddBinding('builtin')}
                onRemove={handleRemoveBinding}
              />
              <ConfigSection
                title="Skill"
                description="可复用的办公任务能力"
                type="skill"
                items={itemsOfType('skill')}
                options={catalog.skill}
                selectedId={selectedToAdd.skill}
                onSelectedChange={(value) => setSelectedToAdd((prev) => ({ ...prev, skill: value }))}
                onAdd={() => handleAddBinding('skill')}
                onRemove={handleRemoveBinding}
              />
              <ConfigSection
                title="MCP 服务"
                description="接入外部工具服务器"
                type="mcp"
                items={itemsOfType('mcp')}
                options={catalog.mcp}
                selectedId={selectedToAdd.mcp}
                onSelectedChange={(value) => setSelectedToAdd((prev) => ({ ...prev, mcp: value }))}
                onAdd={() => handleAddBinding('mcp')}
                onRemove={handleRemoveBinding}
              />
              <ConfigSection
                title="知识库"
                description="让智能体检索指定知识内容"
                type="kb"
                items={itemsOfType('kb')}
                options={catalog.kb}
                selectedId={selectedToAdd.kb}
                onSelectedChange={(value) => setSelectedToAdd((prev) => ({ ...prev, kb: value }))}
                onAdd={() => handleAddBinding('kb')}
                onRemove={handleRemoveBinding}
              />
            </div>

            <ConfigSection
              title="协作智能体"
              description="配置可协作调用的其他智能体"
              type="collaborator"
              items={itemsOfType('collaborator')}
              options={catalog.collaborator}
              selectedId={selectedToAdd.collaborator}
              onSelectedChange={(value) => setSelectedToAdd((prev) => ({ ...prev, collaborator: value }))}
              onAdd={() => handleAddBinding('collaborator')}
              onRemove={handleRemoveBinding}
            />

            <div className="flex items-center justify-end gap-2 pt-4">
              <Button variant="secondary" onClick={() => navigate('/agents')}>
                取消
              </Button>
              <Button variant="primary" onClick={handleSave} disabled={saving || !formData.name.trim()}>
                {saving ? '保存中...' : '保存草稿'}
              </Button>
            </div>
          </div>
        </div>
      </div>
      <PublishDialog
        open={showPublishDialog}
        publishing={publishing}
        onClose={() => setShowPublishDialog(false)}
        onConfirm={handlePublish}
      />
    </Layout>
  )
}

export default AgentEditPage
