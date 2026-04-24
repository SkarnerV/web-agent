import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Settings, Users, Rocket, FileText, Check, Search, Wrench, Globe, Database, Code, Mail, Calendar } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'

// Step types
type StepType = 1 | 2 | 3 | 4

// Tool data
const toolsData = [
  { id: '1', name: '网络搜索', description: '搜索互联网获取最新信息', icon: Globe, enabled: true },
  { id: '2', name: '数据库查询', description: '连接数据库执行查询操作', icon: Database, enabled: false },
  { id: '3', name: '代码执行', description: '在沙箱环境中执行代码', icon: Code, enabled: true },
  { id: '4', name: '邮件发送', description: '发送邮件通知用户', icon: Mail, enabled: false },
  { id: '5', name: '日程管理', description: '管理日程和提醒', icon: Calendar, enabled: true },
  { id: '6', name: '文件操作', description: '读写文件系统', icon: Wrench, enabled: false },
]

// Steps column component
const StepsColumn: React.FC<{ activeStep: StepType }> = ({ activeStep }) => {
  const steps = [
    { step: 1, icon: FileText, label: '基本信息' },
    { step: 2, icon: Settings, label: '工具配置' },
    { step: 3, icon: Users, label: '协作设置' },
    { step: 4, icon: Rocket, label: '发布设置' },
  ]

  return (
    <div className="w-[240px] p-6 bg-white border border-border-subtle rounded-lg flex flex-col gap-2">
      <span className="text-xs font-semibold text-text-tertiary">配置步骤</span>
      {steps.map((s) => (
        <div
          key={s.step}
          className={`flex items-center gap-2.5 p-2.5 px-3 rounded-lg transition-colors ${
            activeStep === s.step
              ? 'bg-brand-50'
              : 'bg-transparent'
          }`}
        >
          <div className={`w-5 h-5 rounded-full flex items-center justify-center ${
            activeStep === s.step
              ? 'bg-brand-500 text-white'
              : 'bg-gray-200 text-text-tertiary'
          }`}>
            {activeStep === s.step ? (
              <Check className="w-3 h-3" />
            ) : (
              <s.icon className="w-3 h-3" />
            )}
          </div>
          <span className={`text-sm ${
            activeStep === s.step
              ? 'font-semibold text-brand-500'
              : 'text-text-secondary'
          }`}>
            {s.label}
          </span>
        </div>
      ))}
      {/* Save tip */}
      <div className="mt-2 p-4 px-3 bg-gray-50 rounded-lg flex flex-col gap-1">
        <span className="text-xs text-text-tertiary">💡 草稿自动保存</span>
        <span className="text-xs text-text-tertiary">上次保存: 5分钟前</span>
      </div>
    </div>
  )
}

// Toggle switch component
const ToggleSwitch: React.FC<{ enabled: boolean; onChange: () => void }> = ({ enabled, onChange }) => {
  return (
    <button
      onClick={onChange}
      className={`w-11 h-6 rounded-full transition-colors ${
        enabled ? 'bg-brand-500' : 'bg-gray-200'
      }`}
    >
      <div className={`w-5 h-5 rounded-full bg-white shadow-sm transition-transform ${
        enabled ? 'translate-x-5' : 'translate-x-0.5'
      }`} />
    </button>
  )
}

// AgentCreateToolsPage Component
const AgentCreateToolsPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [tools, setTools] = useState(toolsData)
  const activeStep: StepType = 2

  const handleBack = () => {
    navigate('/agents/create')
  }

  const handleNextStep = () => {
    navigate('/agents/collab')
  }

  const handlePrevStep = () => {
    navigate('/agents/create')
  }

  const handleToggleTool = (toolId: string) => {
    setTools(tools.map((t) => t.id === toolId ? { ...t, enabled: !t.enabled } : t))
  }

  const filteredTools = tools.filter((tool) =>
    tool.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const enabledCount = tools.filter((t) => t.enabled).length

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '创建' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center justify-between px-6">
        <div className="flex items-center gap-3">
          <button
            onClick={handleBack}
            className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
          >
            <ChevronLeft className="w-4 h-4" />
            <span className="text-sm">返回</span>
          </button>
        </div>
        <span className="text-sm font-semibold text-text-primary flex-1 text-center">创建智能体</span>
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={handlePrevStep}>上一步</Button>
          <Button variant="primary" onClick={handleNextStep}>下一步</Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        {/* Steps Column */}
        <StepsColumn activeStep={activeStep} />

        {/* Form Area */}
        <div className="flex-1 p-8 overflow-auto">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5">
            {/* Search */}
            <div className="flex items-center gap-4">
              <Input
                type="search"
                placeholder="搜索工具"
                value={searchQuery}
                onChange={setSearchQuery}
                icon={<Search className="w-4 h-4" />}
                className="w-[300px]"
              />
              <span className="text-sm text-text-tertiary">
                已选择 {enabledCount} 个工具
              </span>
            </div>

            {/* Tool List */}
            <div className="flex flex-col gap-3">
              {filteredTools.map((tool) => {
                const IconComponent = tool.icon
                return (
                  <div
                    key={tool.id}
                    className="flex items-center gap-4 p-4 rounded-lg border border-border-subtle bg-white hover:border-border-strong transition-colors"
                  >
                    <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center">
                      <IconComponent className="w-5 h-5 text-brand-500" />
                    </div>
                    <div className="flex-1 flex flex-col gap-1">
                      <span className="text-sm font-semibold text-text-primary">{tool.name}</span>
                      <span className="text-xs text-text-tertiary">{tool.description}</span>
                    </div>
                    <ToggleSwitch
                      enabled={tool.enabled}
                      onChange={() => handleToggleTool(tool.id)}
                    />
                  </div>
                )
              })}
            </div>

            {/* Empty state */}
            {filteredTools.length === 0 && (
              <div className="flex flex-col items-center justify-center py-8">
                <Search className="w-8 h-8 text-text-tertiary mb-2" />
                <span className="text-sm text-text-secondary">未找到匹配的工具</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreateToolsPage