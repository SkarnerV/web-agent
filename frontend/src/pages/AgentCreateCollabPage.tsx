import React from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Check, ListOrdered } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { saveAgentWizardDraft } from './agentWizardDraft'

type StepType = 1 | 2 | 3 | 4

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
            {activeStep === s.step || s.step < activeStep ? (
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

const errorStrategies = [
  { id: 'retry', label: '自动重试', description: '失败后自动重试最多3次' },
  { id: 'skip', label: '跳过继续', description: '跳过失败步骤继续执行' },
  { id: 'fallback', label: '降级处理', description: '使用备用方案替代' },
]

const AgentCreateCollabPage: React.FC = () => {
  const navigate = useNavigate()
  const [errorStrategy, setErrorStrategy] = React.useState('retry')

  React.useEffect(() => {
    saveAgentWizardDraft({ collabMode: 'sequential', errorStrategy })
  }, [errorStrategy])

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '创建' }]}>
      {/* TopBar */}
      <div className="h-14 bg-white border-b border-border-subtle flex items-center gap-3 px-6">
        <button
          onClick={() => navigate('/agents/tools')}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回</span>
        </button>
        <span className="text-[15px] font-semibold text-text-primary flex-1">创建智能体</span>
      </div>

      {/* Body */}
      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        <StepsColumn activeStep={3} />

        <div className="flex-1 overflow-auto">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5">
            {/* Header */}
            <div className="flex flex-col gap-1">
              <h2 className="text-lg font-bold text-text-primary">协作智能体</h2>
              <p className="text-[13px] text-text-tertiary">
                配置子任务执行策略与异常处理方式
              </p>
            </div>

            {/* Collaboration Mode - Sequential Only */}
            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">协作模式</label>
              <div className="flex items-center gap-3 p-4 rounded-lg border border-brand-500 bg-brand-50">
                <div className="w-10 h-10 rounded-lg bg-brand-500 flex items-center justify-center">
                  <ListOrdered className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1 flex flex-col gap-1">
                  <span className="text-[13px] font-semibold text-brand-500">顺序执行</span>
                  <span className="text-[11px] text-text-tertiary">智能体按顺序依次执行任务</span>
                </div>
                <div className="w-5 h-5 rounded-full bg-brand-500 flex items-center justify-center">
                  <Check className="w-3 h-3 text-white" />
                </div>
              </div>
            </div>

            {/* Error Handling Strategy */}
            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">异常处理策略</label>
              <div className="flex gap-2.5">
                {errorStrategies.map((s) => (
                  <button
                    key={s.id}
                    onClick={() => setErrorStrategy(s.id)}
                    className={`flex flex-col gap-1 p-4 rounded-lg border transition-all text-left ${
                      errorStrategy === s.id
                        ? 'border-brand-500 bg-brand-50'
                        : 'border-border-subtle bg-white hover:border-border-strong'
                    }`}
                  >
                    <span
                      className={`text-[13px] font-semibold ${
                        errorStrategy === s.id ? 'text-brand-500' : 'text-text-primary'
                      }`}
                    >
                      {s.label}
                    </span>
                    <span className="text-[11px] text-text-tertiary">{s.description}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Bottom Row */}
            <div className="flex items-center justify-end gap-2 pt-4">
              <Button variant="secondary" onClick={() => navigate('/agents/tools')}>
                上一步
              </Button>
              <Button variant="primary" onClick={() => navigate('/agents/publish')}>
                下一步
              </Button>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreateCollabPage
