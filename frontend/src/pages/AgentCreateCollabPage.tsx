import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Check, Plus, X, Bot } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

type StepType = 1 | 2 | 3 | 4
type CollabMode = 'sequential' | 'parallel' | 'conditional'

interface SubAgent {
  id: string
  name: string
  role: string
}

interface Member {
  id: string
  name: string
  avatar: string
  role: string
  permission: 'admin' | 'editor' | 'viewer'
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

const collabModes: { id: CollabMode; label: string; description: string }[] = [
  { id: 'sequential', label: '顺序执行', description: '智能体按顺序依次执行任务' },
  { id: 'parallel', label: '并行执行', description: '多个子智能体同时执行任务' },
  { id: 'conditional', label: '条件调度', description: '根据条件动态选择执行的智能体' },
]

const errorStrategies = [
  { id: 'retry', label: '自动重试', description: '失败后自动重试最多3次' },
  { id: 'skip', label: '跳过继续', description: '跳过失败步骤继续执行' },
  { id: 'fallback', label: '降级处理', description: '使用备用方案替代' },
]

const AgentCreateCollabPage: React.FC = () => {
  const navigate = useNavigate()
  const [collabMode, setCollabMode] = useState<CollabMode>('sequential')
  const [errorStrategy, setErrorStrategy] = useState('retry')
  const [subAgents] = useState<SubAgent[]>([
    { id: '1', name: '数据分析助手', role: '分析销售数据并生成报告' },
    { id: '2', name: '图表生成器', role: '将数据转换为可视化图表' },
  ])
  const [members, setMembers] = useState<Member[]>([
    { id: '1', name: '张三', avatar: 'Z', role: '创建者', permission: 'admin' },
    { id: '2', name: '李四', avatar: 'L', role: '开发者', permission: 'editor' },
    { id: '3', name: '王五', avatar: 'W', role: '测试人员', permission: 'viewer' },
  ])

  const handlePermissionChange = (memberId: string, permission: 'admin' | 'editor' | 'viewer') => {
    setMembers(members.map((m) => (m.id === memberId ? { ...m, permission } : m)))
  }

  const handleRemoveMember = (memberId: string) => {
    setMembers(members.filter((m) => m.id !== memberId))
  }

  const handleAddMember = () => {
    const newMember: Member = {
      id: `${Date.now()}`,
      name: '新成员',
      avatar: 'N',
      role: '协作者',
      permission: 'viewer',
    }
    setMembers([...members, newMember])
  }

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
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={() => navigate('/agents/tools')}>
            保存草稿
          </Button>
          <Button variant="primary">发布</Button>
        </div>
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
                配置多个智能体的子任务协作，定义调度策略与误差处理
              </p>
            </div>

            {/* Collaboration Mode */}
            <div className="flex flex-col gap-2">
              <label className="text-[13px] font-medium text-text-primary">协作模式</label>
              <div className="flex gap-2.5">
                {collabModes.map((mode) => (
                  <button
                    key={mode.id}
                    onClick={() => setCollabMode(mode.id)}
                    className={`flex flex-col gap-1 p-4 rounded-lg border transition-all text-left ${
                      collabMode === mode.id
                        ? 'border-brand-500 bg-brand-50'
                        : 'border-border-subtle bg-white hover:border-border-strong'
                    }`}
                  >
                    <span
                      className={`text-[13px] font-semibold ${
                        collabMode === mode.id ? 'text-brand-500' : 'text-text-primary'
                      }`}
                    >
                      {mode.label}
                    </span>
                    <span className="text-[11px] text-text-tertiary">{mode.description}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Sub-Agent List */}
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-2">
                <Bot className="w-4 h-4 text-text-secondary" />
                <span className="text-[13px] font-semibold text-text-primary">子智能体</span>
              </div>
              {subAgents.map((agent) => (
                <div
                  key={agent.id}
                  className="flex items-center gap-3 px-3.5 py-3 bg-white border border-border-subtle rounded-lg"
                >
                  <div className="w-8 h-8 rounded-full bg-brand-500 flex items-center justify-center">
                    <Bot className="w-4 h-4 text-white" />
                  </div>
                  <div className="flex-1 flex flex-col gap-0.5">
                    <span className="text-[13px] font-semibold text-text-primary">{agent.name}</span>
                    <span className="text-[11px] text-text-tertiary">{agent.role}</span>
                  </div>
                </div>
              ))}
              <button className="flex items-center gap-1.5 text-[13px] text-brand-500 hover:text-brand-600 transition-colors">
                <Plus className="w-3.5 h-3.5" />
                添加子智能体
              </button>
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

            {/* Divider */}
            <div className="h-px bg-border-subtle" />

            {/* Team Members */}
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-[13px] font-semibold text-text-primary">团队成员</span>
                <button
                  onClick={handleAddMember}
                  className="flex items-center gap-1 text-[13px] text-brand-500 hover:text-brand-600 transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                  添加成员
                </button>
              </div>
              {members.map((member) => (
                <div
                  key={member.id}
                  className="flex items-center gap-4 p-4 rounded-lg border border-border-subtle bg-white"
                >
                  <div className="w-10 h-10 rounded-full bg-brand-500 flex items-center justify-center text-white font-semibold text-sm">
                    {member.avatar}
                  </div>
                  <div className="flex-1 flex flex-col gap-0.5">
                    <span className="text-sm font-semibold text-text-primary">{member.name}</span>
                    <span className="text-xs text-text-tertiary">{member.role}</span>
                  </div>
                  <select
                    value={member.permission}
                    onChange={(e) =>
                      handlePermissionChange(member.id, e.target.value as 'admin' | 'editor' | 'viewer')
                    }
                    disabled={member.role === '创建者'}
                    className={`px-3 py-1.5 rounded-md text-sm border border-border-subtle bg-white ${
                      member.role === '创建者' ? 'opacity-50 cursor-not-allowed' : 'hover:border-border-strong'
                    }`}
                  >
                    <option value="admin">管理员</option>
                    <option value="editor">编辑者</option>
                    <option value="viewer">查看者</option>
                  </select>
                  {member.role !== '创建者' && (
                    <button
                      onClick={() => handleRemoveMember(member.id)}
                      className="w-8 h-8 rounded-md flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 transition-colors"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
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
