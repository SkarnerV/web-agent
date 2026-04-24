import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Settings, Users, Rocket, FileText, Check, Globe, Lock } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

// Step types
type StepType = 1 | 2 | 3 | 4

// Publish scope types
type PublishScopeType = 'public' | 'private'

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
        <span className="text-xs text-text-tertiary">上次保存: 1分钟前</span>
      </div>
    </div>
  )
}

// Publish scope card component
const PublishScopeCard: React.FC<{
  type: PublishScopeType
  active: boolean
  onClick: () => void
}> = ({ type, active, onClick }) => {
  const config = {
    public: {
      icon: Globe,
      label: '公开发布',
      description: '所有人都可以查看和使用此智能体',
      iconBg: 'bg-brand-50',
      iconColor: 'text-brand-500',
    },
    private: {
      icon: Lock,
      label: '私有发布',
      description: '仅团队成员可以查看和使用',
      iconBg: 'bg-gray-100',
      iconColor: 'text-gray-600',
    },
  }

  const { icon: IconComponent, label, description, iconBg, iconColor } = config[type]

  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-4 p-4 rounded-lg border transition-all ${
        active
          ? 'border-brand-500 bg-brand-50'
          : 'border-border-subtle bg-white hover:border-border-strong'
      }`}
    >
      <div className={`w-10 h-10 rounded-lg ${iconBg} flex items-center justify-center`}>
        <IconComponent className={`w-5 h-5 ${iconColor}`} />
      </div>
      <div className="flex-1 flex flex-col gap-1">
        <span className={`text-sm font-semibold ${
          active ? 'text-brand-500' : 'text-text-primary'
        }`}>
          {label}
        </span>
        <span className="text-xs text-text-tertiary">{description}</span>
      </div>
      {active && (
        <div className="w-5 h-5 rounded-full bg-brand-500 flex items-center justify-center">
          <Check className="w-3 h-3 text-white" />
        </div>
      )}
    </button>
  )
}

// AgentCreatePublishPage Component
const AgentCreatePublishPage: React.FC = () => {
  const navigate = useNavigate()
  const [publishScope, setPublishScope] = useState<PublishScopeType>('public')
  const [publishNotes, setPublishNotes] = useState('')
  const activeStep: StepType = 4

  const handleBack = () => {
    navigate('/agents/collab')
  }

  const handlePrevStep = () => {
    navigate('/agents/collab')
  }

  const handlePublish = () => {
    console.log('Publish:', { scope: publishScope, notes: publishNotes })
    navigate('/agents')
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
          <Button variant="secondary" onClick={handlePrevStep}>上一步</Button>
          <Button variant="primary" icon={<Rocket className="w-4 h-4" />} onClick={handlePublish}>
            发布
          </Button>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        {/* Steps Column */}
        <StepsColumn activeStep={activeStep} />

        {/* Form Area */}
        <div className="flex-1 p-8 overflow-auto">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5 max-w-[600px]">
            {/* Publish scope */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-text-primary">发布范围</label>
              <div className="flex flex-col gap-3">
                <PublishScopeCard
                  type="public"
                  active={publishScope === 'public'}
                  onClick={() => setPublishScope('public')}
                />
                <PublishScopeCard
                  type="private"
                  active={publishScope === 'private'}
                  onClick={() => setPublishScope('private')}
                />
              </div>
            </div>

            {/* Publish notes */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-text-primary">发布说明</label>
              <textarea
                placeholder="描述此次发布的更新内容或使用说明..."
                value={publishNotes}
                onChange={(e) => setPublishNotes(e.target.value)}
                className="w-full p-2.5 px-3 bg-white border border-border-strong rounded-sm text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-[120px]"
              />
            </div>

            {/* Info tip */}
            <div className="p-4 bg-gray-50 rounded-lg">
              <span className="text-xs text-text-tertiary">
                发布后，智能体将可以被指定范围内的用户使用。如需修改，可以随时编辑并重新发布。
              </span>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreatePublishPage