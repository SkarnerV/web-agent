import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Check, Search, Wand, Plug, BookOpen, Plus } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'

type StepType = 1 | 2 | 3 | 4

interface ToolItem {
  id: string
  name: string
  description: string
  type: 'skill' | 'mcp' | 'kb'
  enabled: boolean
}

const initialTools: ToolItem[] = [
  { id: '1', name: '代码格式化', description: '自动格式化多种语言代码', type: 'skill', enabled: true },
  { id: '2', name: 'JSON 解析', description: '解析和处理 JSON 数据', type: 'skill', enabled: false },
  { id: '3', name: 'Playwright MCP', description: '浏览器自动化和网页测试', type: 'mcp', enabled: true },
  { id: '4', name: 'GitHub MCP', description: '代码仓库操作工具', type: 'mcp', enabled: false },
  { id: '5', name: '产品手册', description: '产品功能和使用文档', type: 'kb', enabled: true },
  { id: '6', name: '技术规范', description: '内部技术标准和规范', type: 'kb', enabled: false },
]

const sectionConfig = {
  skill: { icon: Wand, label: 'Skill', addText: '添加 Skill' },
  mcp: { icon: Plug, label: 'MCP 服务', addText: '添加 MCP' },
  kb: { icon: BookOpen, label: '知识库', addText: '添加知识库' },
}

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
              activeStep === s.step
                ? 'bg-brand-500 text-white'
                : s.step < activeStep
                ? 'bg-success-500 text-white'
                : 'bg-gray-100 text-text-tertiary'
            }`}
          >
            {activeStep === s.step ? (
              <Check className="w-3 h-3" />
            ) : s.step < activeStep ? (
              <span className="text-xs font-semibold">{s.step}</span>
            ) : (
              <span className="text-xs font-semibold">{s.step}</span>
            )}
          </div>
          <span
            className={`text-[13px] ${
              activeStep === s.step
                ? 'font-semibold text-brand-500'
                : 'text-text-secondary'
            }`}
          >
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

const ToggleSwitch: React.FC<{ enabled: boolean; onChange: () => void }> = ({ enabled, onChange }) => (
  <button
    onClick={onChange}
    className={`w-11 h-6 rounded-full transition-colors ${
      enabled ? 'bg-brand-500' : 'bg-gray-200'
    }`}
  >
    <div
      className={`w-5 h-5 rounded-full bg-white shadow-sm transition-transform ${
        enabled ? 'translate-x-5' : 'translate-x-0.5'
      }`}
    />
  </button>
)

const AgentCreateToolsPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [tools, setTools] = useState(initialTools)

  const handleToggleTool = (toolId: string) => {
    setTools(tools.map((t) => (t.id === toolId ? { ...t, enabled: !t.enabled } : t)))
  }

  const filteredTools = tools.filter((t) =>
    t.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const skills = filteredTools.filter((t) => t.type === 'skill')
  const mcps = filteredTools.filter((t) => t.type === 'mcp')
  const kbs = filteredTools.filter((t) => t.type === 'kb')

  const renderSection = (type: 'skill' | 'mcp' | 'kb', items: ToolItem[]) => {
    const cfg = sectionConfig[type]
    const SectionIcon = cfg.icon
    return (
      <div className="flex flex-col gap-3">
        <div className="flex items-center gap-2">
          <SectionIcon className="w-4 h-4 text-text-secondary" />
          <span className="text-[13px] font-semibold text-text-primary">{cfg.label}</span>
        </div>
        {items.length === 0 ? (
          <div className="text-[13px] text-text-tertiary py-2">暂无匹配项</div>
        ) : (
          <div className="flex flex-col gap-2">
            {items.map((tool) => (
              <div
                key={tool.id}
                className="flex items-center gap-3 px-3.5 py-3 bg-white border border-border-subtle rounded-lg hover:border-border-strong transition-colors"
              >
                <div className="flex-1 flex flex-col gap-0.5">
                  <span className="text-[13px] font-semibold text-text-primary">{tool.name}</span>
                  <span className="text-[11px] text-text-tertiary">{tool.description}</span>
                </div>
                <ToggleSwitch
                  enabled={tool.enabled}
                  onChange={() => handleToggleTool(tool.id)}
                />
              </div>
            ))}
          </div>
        )}
        <button className="flex items-center gap-1.5 text-[13px] text-brand-500 hover:text-brand-600 transition-colors">
          <Plus className="w-3.5 h-3.5" />
          {cfg.addText}
        </button>
      </div>
    )
  }

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
          <Button variant="secondary" onClick={() => navigate('/agents/create')}>
            保存草稿
          </Button>
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
                为智能体添加 Skill、MCP 服务、知识库引用
              </p>
            </div>

            {/* Search */}
            <div className="w-[300px]">
              <Input
                type="search"
                placeholder="搜索工具"
                value={searchQuery}
                onChange={setSearchQuery}
                icon={<Search className="w-4 h-4" />}
              />
            </div>

            {/* Skill Section */}
            {renderSection('skill', skills)}

            {/* Divider */}
            <div className="h-px bg-border-subtle" />

            {/* MCP Section */}
            {renderSection('mcp', mcps)}

            {/* Divider */}
            <div className="h-px bg-border-subtle" />

            {/* Knowledge Base Section */}
            {renderSection('kb', kbs)}

            {/* Bottom Row */}
            <div className="flex items-center justify-end gap-2 pt-4">
              <Button variant="secondary" onClick={() => navigate('/agents/create')}>
                上一步
              </Button>
              <Button variant="primary" onClick={() => navigate('/agents/collab')}>
                下一步
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreateToolsPage
