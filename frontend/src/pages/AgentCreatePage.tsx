import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Check, Bot, Sparkles, Zap, MessageSquare } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { createAgent } from '../api/agent'
import { ApiError } from '../api/client'

type StepType = 1 | 2 | 3 | 4

const iconOptions = [
  { id: 'bot', icon: Bot, bg: 'bg-brand-50', color: 'text-brand-500' },
  { id: 'sparkles', icon: Sparkles, bg: 'bg-warning-50', color: 'text-warning-500' },
  { id: 'zap', icon: Zap, bg: 'bg-success-50', color: 'text-success-500' },
  { id: 'message', icon: MessageSquare, bg: 'bg-error-50', color: 'text-error-500' },
]

const modelOptions = [
  { id: 'claude-opus', label: 'Claude Opus 4.7' },
  { id: 'claude-sonnet', label: 'Claude Sonnet 4.6' },
  { id: 'gpt-4o', label: 'GPT-4o' },
]

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
                : 'bg-gray-100 text-text-tertiary'
            }`}
          >
            {activeStep === s.step ? (
              <Check className="w-3 h-3" />
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

const AgentCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    icon: 'bot',
    model: 'claude-sonnet',
    prompt: '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleBack = () => navigate('/agents')

  const handleSaveDraft = async () => {
    if (!formData.name.trim()) return
    setSaving(true)
    setError(null)
    try {
      await createAgent({
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
        avatar: formData.icon,
      })
      navigate('/agents')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '创建失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  const handleNext = () => {
    navigate('/agents/tools')
  }

  const canSave = formData.name.trim().length > 0

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '创建' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center gap-3 px-6">
        <button
          onClick={handleBack}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回</span>
        </button>
        <span className="text-[15px] font-semibold text-text-primary flex-1">创建智能体</span>
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={handleSaveDraft} disabled={!canSave || saving}>
            {saving ? '保存中...' : '保存草稿'}
          </Button>
          <Button variant="primary" onClick={handleSaveDraft} disabled={!canSave || saving}>
            发布
          </Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        <StepsColumn activeStep={1} />

        {/* Form Area */}
        <div className="flex-1 overflow-auto">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5">
            {/* Form Header */}
            <div className="flex flex-col gap-1">
              <h2 className="text-lg font-bold text-text-primary">基本信息</h2>
              <p className="text-[13px] text-text-tertiary">设置智能体的基础身份和行为</p>
            </div>

            {error && (
              <div className="px-3 py-2 bg-error-50 border border-error-200 rounded text-sm text-error-500">
                {error}
              </div>
            )}

            {/* Avatar Picker */}
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

            {/* Name Field */}
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

            {/* Description Field */}
            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">描述</label>
              <textarea
                placeholder="描述智能体的功能和使用场景"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-[72px] focus:border-brand-500"
              />
            </div>

            {/* Two-Column: Model + Step */}
            <div className="flex gap-4">
              <div className="flex-1 flex flex-col gap-2">
                <label className="text-[13px] font-medium text-text-primary">模型</label>
                <select
                  value={formData.model}
                  onChange={(e) => setFormData({ ...formData, model: e.target.value })}
                  className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary outline-none focus:border-brand-500"
                >
                  {modelOptions.map((m) => (
                    <option key={m.id} value={m.id}>{m.label}</option>
                  ))}
                </select>
              </div>
              <div className="flex-1 flex flex-col gap-2">
                <label className="text-[13px] font-medium text-text-primary">当前步骤</label>
                <div className="w-full px-3 py-2.5 bg-gray-50 border border-border-strong rounded-md text-sm text-text-secondary">
                  基本信息
                </div>
              </div>
            </div>

            {/* Prompt Field */}
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

            {/* Bottom Row */}
            <div className="flex items-center justify-end gap-2 pt-4">
              <Button variant="secondary" onClick={handleBack}>
                取消
              </Button>
              <Button variant="primary" onClick={handleNext} disabled={!canSave}>
                下一步
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreatePage
