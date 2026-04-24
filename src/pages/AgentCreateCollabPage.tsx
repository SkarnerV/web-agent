import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, Settings, Users, Rocket, FileText, Check, UserPlus, X } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

// Step types
type StepType = 1 | 2 | 3 | 4

// Permission types
type PermissionType = 'admin' | 'editor' | 'viewer'

// Member data
const membersData = [
  { id: '1', name: '张三', avatar: 'Z', role: '创建者', permission: 'admin' as PermissionType },
  { id: '2', name: '李四', avatar: 'L', role: '开发者', permission: 'editor' as PermissionType },
  { id: '3', name: '王五', avatar: 'W', role: '测试人员', permission: 'viewer' as PermissionType },
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
        <span className="text-xs text-text-tertiary">上次保存: 2分钟前</span>
      </div>
    </div>
  )
}

// Permission select component
const PermissionSelect: React.FC<{
  value: PermissionType
  onChange: (value: PermissionType) => void
  disabled?: boolean
}> = ({ value, onChange, disabled = false }) => {
  const options = [
    { value: 'admin', label: '管理员' },
    { value: 'editor', label: '编辑者' },
    { value: 'viewer', label: '查看者' },
  ]

  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value as PermissionType)}
      disabled={disabled}
      className={`px-3 py-1.5 rounded-md text-sm border border-border-subtle bg-white ${
        disabled ? 'opacity-50 cursor-not-allowed' : 'hover:border-border-strong'
      }`}
    >
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
  )
}

// Member row component
const MemberRow: React.FC<{
  member: typeof membersData[0]
  onPermissionChange: (permission: PermissionType) => void
  onRemove?: () => void
}> = ({ member, onPermissionChange, onRemove }) => {
  return (
    <div className="flex items-center gap-4 p-4 rounded-lg border border-border-subtle bg-white">
      {/* Avatar */}
      <div className="w-10 h-10 rounded-full bg-brand-500 flex items-center justify-center text-white font-semibold text-sm">
        {member.avatar}
      </div>
      {/* Info */}
      <div className="flex-1 flex flex-col gap-0.5">
        <span className="text-sm font-semibold text-text-primary">{member.name}</span>
        <span className="text-xs text-text-tertiary">{member.role}</span>
      </div>
      {/* Permission */}
      <PermissionSelect
        value={member.permission}
        onChange={onPermissionChange}
        disabled={member.role === '创建者'}
      />
      {/* Remove */}
      {member.role !== '创建者' && onRemove && (
        <button
          onClick={onRemove}
          className="w-8 h-8 rounded-md flex items-center justify-center text-text-tertiary hover:text-error-500 hover:bg-error-50 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  )
}

// AgentCreateCollabPage Component
const AgentCreateCollabPage: React.FC = () => {
  const navigate = useNavigate()
  const [members, setMembers] = useState(membersData)
  const activeStep: StepType = 3

  const handleBack = () => {
    navigate('/agents/tools')
  }

  const handleNextStep = () => {
    navigate('/agents/publish')
  }

  const handlePrevStep = () => {
    navigate('/agents/tools')
  }

  const handlePermissionChange = (memberId: string, permission: PermissionType) => {
    setMembers(members.map((m) => m.id === memberId ? { ...m, permission } : m))
  }

  const handleRemoveMember = (memberId: string) => {
    setMembers(members.filter((m) => m.id !== memberId))
  }

  const handleAddMember = () => {
    // Mock add member
    const newMember = {
      id: `${Date.now()}`,
      name: '新成员',
      avatar: 'N',
      role: '协作者',
      permission: 'viewer' as PermissionType,
    }
    setMembers([...members, newMember])
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
            {/* Add member button */}
            <Button
              variant="secondary"
              icon={<UserPlus className="w-4 h-4" />}
              onClick={handleAddMember}
            >
              添加成员
            </Button>

            {/* Member list */}
            <div className="flex flex-col gap-3">
              {members.map((member) => (
                <MemberRow
                  key={member.id}
                  member={member}
                  onPermissionChange={(permission) => handlePermissionChange(member.id, permission)}
                  onRemove={() => handleRemoveMember(member.id)}
                />
              ))}
            </div>

            {/* Empty state */}
            {members.length === 1 && (
              <div className="text-sm text-text-tertiary py-4 text-center">
                当前只有创建者一人，可以添加更多协作者
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentCreateCollabPage