import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Settings, Users, Rocket, FileText, Check, Bot, Sparkles, Zap, MessageSquare } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'

// Step types
type StepType = 1 | 2 | 3 | 4

// Icon options for agent
const iconOptions = [
  { id: 'bot', icon: Bot, bg: 'bg-brand-50', color: 'text-brand-500' },
  { id: 'sparkles', icon: Sparkles, bg: 'bg-warning-50', color: 'text-warning-500' },
  { id: 'zap', icon: Zap, bg: 'bg-success-50', color: 'text-success-500' },
  { id: 'message', icon: MessageSquare, bg: 'bg-error-50', color: 'text-error-500' },
]

// Type options for agent
const typeOptions = [
  { id: 'chat', label: '对话型', description: '适合问答、咨询、客服场景' },
  { id: 'workflow', label: '工作流型', description: '适合自动化任务执行' },
  { id: 'analysis', label: '分析型', description: '适合数据分析、报告生成' },
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
        <span className="text-xs text-text-tertiary">上次保存: 10分钟前</span>
      </div>
    </div>
  )
}

// AgentCreatePage Component
const AgentCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    icon: 'bot',
    type: 'chat',
  })
  const activeStep: StepType = 1

  const handleBack = () => {
    navigate('/agents')
  }

  const handleNextStep = () => {
    navigate('/agents/tools')
  }

  const handleSaveDraft = () => {
    console.log('Save draft:', formData)
  }

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
          <Button variant="secondary" onClick={handleSaveDraft}>保存草稿</Button>
          <Button variant="primary" onClick={handleNextStep}>下一步</Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        {/* Steps Column */}
        <StepsColumn activeStep={activeStep} />

        {/* Form Area */}
        <div className="flex-1 p-8">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5 max-w-[600px]">
            {/* Name */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-text-primary">智能体名称</label>
              <Input
                placeholder="输入智能体名称"
                value={formData.name}
                onChange={(value) => setFormData({ ...formData, name: value })}
              />
            </div>

            {/* Description */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-text-primary">描述</label>
              <textarea
                placeholder="描述智能体的功能和使用场景"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full p-2.5 px-3 bg-white border border-border-strong rounded-sm text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-[100px]"
              />
            </div>

            {/* Icon Selection */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-text-primary">图标</label>
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
                      <IconComponent className={`w-5 h-5 ${formData.icon === opt.id ? opt.color : 'text-gray-600'}`} />
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Type Selection */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-text-primary">类型</label>
              <div className="flex items-center gap-3">
                {typeOptions.map((opt) => (
                  <button
                    key={opt.id}
                    onClick={() => setFormData({ ...formData, type: opt.id })}
                    className={`flex flex-col gap-1 p-4 rounded-lg border transition-all ${
                      formData.type === opt.id
                        ? 'border-brand-500 bg-brand-50'
                        : 'border-border-subtle bg-white hover:border-border-strong'
                    }`}
                  >
                    <span className={`text-sm font-semibold ${
                      formData.type === opt.id ? 'text-brand-500' : 'text-text-primary'
                    }`}>
                      {opt.label}
                    </span>
                    <span className="text-xs text-text-tertiary">{opt.description}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreatePage