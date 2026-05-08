import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Check, Rocket, Globe, Lock, Users } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { createAgent } from '../api/agent'
import { ApiError } from '../api/client'
import {
  clearAgentWizardDraft,
  readAgentWizardDraft,
  saveAgentWizardDraft,
  toAgentCreateRequest,
} from './agentWizardDraft'

type StepType = 1 | 2 | 3 | 4
type PublishScopeType = 'private' | 'team' | 'public'

interface ChecklistItem {
  id: string
  label: string
  done: boolean
  status: 'success' | 'warning'
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

const PublishScopeCard: React.FC<{
  type: PublishScopeType
  active: boolean
  onClick: () => void
}> = ({ type, active, onClick }) => {
  const config = {
    private: {
      icon: Lock,
      label: '私人',
      description: '仅自己可以查看和使用此智能体',
      iconBg: 'bg-gray-100',
      iconColor: 'text-gray-600',
    },
    team: {
      icon: Users,
      label: '团队',
      description: '团队成员可以查看和使用此智能体',
      iconBg: 'bg-info-50',
      iconColor: 'text-info-500',
    },
    public: {
      icon: Globe,
      label: '公开',
      description: '所有人都可以查看和使用此智能体',
      iconBg: 'bg-brand-50',
      iconColor: 'text-brand-500',
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
      <div className="flex-1 flex flex-col gap-1 text-left">
        <span
          className={`text-sm font-semibold ${active ? 'text-brand-500' : 'text-text-primary'}`}
        >
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

const AgentCreatePublishPage: React.FC = () => {
  const navigate = useNavigate()
  const initialDraft = readAgentWizardDraft()
  const [publishScope, setPublishScope] = useState<PublishScopeType>(
    initialDraft.visibility === 'private' || initialDraft.visibility === 'team' || initialDraft.visibility === 'public'
      ? initialDraft.visibility
      : 'private',
  )
  const [publishNotes, setPublishNotes] = useState(initialDraft.releaseNotes)
  const [version, setVersion] = useState(initialDraft.version)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const currentDraft = readAgentWizardDraft()
  const hasName = currentDraft.name.trim().length > 0
  const checklist: ChecklistItem[] = [
    { id: '1', label: '基本信息已完整填写', done: hasName, status: hasName ? 'success' : 'warning' },
    { id: '2', label: '工具配置已保存', done: true, status: 'success' },
    {
      id: '3',
      label: '协作模式已选定',
      done: Boolean(currentDraft.collabMode),
      status: currentDraft.collabMode ? 'success' : 'warning',
    },
  ]

  React.useEffect(() => {
    saveAgentWizardDraft({
      visibility: publishScope,
      version,
      releaseNotes: publishNotes,
    })
  }, [publishScope, version, publishNotes])

  const createFromDraft = async () => {
    const draft = saveAgentWizardDraft({
      visibility: publishScope,
      version,
      releaseNotes: publishNotes,
    })
    if (!draft.name.trim()) {
      throw new Error('请先填写智能体名称')
    }
    return createAgent(toAgentCreateRequest(draft))
  }

  const handleSaveDraft = async () => {
    setSaving(true)
    setError(null)
    try {
      await createFromDraft()
      clearAgentWizardDraft()
      navigate('/agents')
    } catch (e) {
      setError(e instanceof ApiError || e instanceof Error ? e.message : '保存失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  const handleStartDebug = async () => {
    setSaving(true)
    setError(null)
    try {
      const agent = await createFromDraft()
      clearAgentWizardDraft()
      navigate(`/agents/debug/${agent.id}`, {
        state: {
          publish: {
            visibility: publishScope,
            version: version.trim() || '1.0.0',
            releaseNotes: publishNotes.trim() || undefined,
          },
        },
      })
    } catch (e) {
      setError(e instanceof ApiError || e instanceof Error ? e.message : '进入调试失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }, { label: '创建' }]}>
      <div className="h-14 bg-white border-b border-border-subtle flex items-center gap-3 px-6">
        <button
          onClick={() => navigate('/agents/collab')}
          className="flex items-center gap-1 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="w-4 h-4" />
          <span className="text-[13px]">返回</span>
        </button>
        <span className="text-[15px] font-semibold text-text-primary flex-1">创建智能体</span>
      </div>

      <div className="flex-1 flex gap-5 p-8 overflow-auto">
        <StepsColumn activeStep={4} />

        <div className="flex-1 overflow-auto">
          <div className="p-8 bg-white rounded-xl border border-border-subtle flex flex-col gap-5">
            <div className="flex flex-col gap-1">
              <h2 className="text-lg font-bold text-text-primary">调试发布</h2>
              <p className="text-[13px] text-text-tertiary">
                验证智能体交付结果并推送到正式环境
              </p>
            </div>

            {error && (
              <div className="px-3 py-2 bg-error-50 border border-error-200 rounded text-sm text-error-500">
                {error}
              </div>
            )}

            <div className="flex flex-col gap-3">
              <label className="text-[13px] font-medium text-text-primary">发布检查清单</label>
              {checklist.map((item) => (
                <div
                  key={item.id}
                  className={`flex items-center gap-2.5 px-3.5 py-3 rounded-lg border ${
                    item.status === 'success'
                      ? 'bg-success-50 border-success-500'
                      : 'bg-warning-50 border-warning-500'
                  }`}
                >
                  <Check
                    className={`w-4 h-4 ${
                      item.done ? 'text-success-500' : 'text-warning-500'
                    }`}
                  />
                  <span className="text-[13px] text-text-primary">{item.label}</span>
                </div>
              ))}
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">版本号</label>
              <input
                type="text"
                placeholder="1.0.0"
                value={version}
                onChange={(e) => setVersion(e.target.value)}
                className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none focus:border-brand-500"
              />
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">发布说明</label>
              <textarea
                placeholder="描述此次发布的更新内容..."
                value={publishNotes}
                onChange={(e) => setPublishNotes(e.target.value)}
                className="w-full px-3 py-2.5 bg-white border border-border-strong rounded-md text-sm text-text-primary placeholder:text-text-tertiary outline-none resize-none h-20 focus:border-brand-500"
              />
            </div>

            <div className="flex flex-col gap-2.5">
              <label className="text-[13px] font-medium text-text-primary">可见范围</label>
              <div className="flex flex-col gap-3">
                <PublishScopeCard
                  type="private"
                  active={publishScope === 'private'}
                  onClick={() => setPublishScope('private')}
                />
                <PublishScopeCard
                  type="team"
                  active={publishScope === 'team'}
                  onClick={() => setPublishScope('team')}
                />
                <PublishScopeCard
                  type="public"
                  active={publishScope === 'public'}
                  onClick={() => setPublishScope('public')}
                />
              </div>
            </div>

            <div className="flex items-center justify-between gap-2 pt-4">
              <Button variant="secondary" onClick={() => navigate('/agents/collab')}>
                上一步
              </Button>
              <div className="flex items-center gap-2">
                <Button variant="secondary" onClick={handleSaveDraft} disabled={saving}>
                  保存草稿
                </Button>
                <Button variant="primary" icon={<Rocket className="w-4 h-4" />} onClick={handleStartDebug} disabled={saving}>
                  {saving ? '准备调试中...' : '开始调试'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreatePublishPage
