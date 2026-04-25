import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Wand } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// Mock data for skills
const skillsData = [
  {
    id: '1',
    name: '代码检视专家',
    description: '启动一个专业的代码检视子代理，按照预定义的检视方向对代码进行深度分析，输出JSON格式的详细检视报告。',
    iconBg: 'bg-brand-50',
    iconText: '代',
    status: 'published' as const,
    agentCount: 3,
    updatedAt: '2天前',
  },
  {
    id: '2',
    name: '开发反思',
    description: '生成一个反思 markdown 文件，捕获 bug fix 过程中发生了什么、尝试了什么、以及关键收获。',
    iconBg: 'bg-warning-50',
    iconText: '反',
    status: 'draft' as const,
    agentCount: 1,
    updatedAt: '5天前',
  },
  {
    id: '3',
    name: 'API文档生成',
    description: '自动从代码注释和类型定义生成完整的 API 文档，支持多种格式输出。',
    iconBg: 'bg-success-50',
    iconText: 'A',
    status: 'published' as const,
    agentCount: 5,
    updatedAt: '1周前',
  },
  {
    id: '4',
    name: '测试覆盖率分析',
    description: '分析项目测试覆盖率，识别缺失的测试场景并生成测试建议。',
    iconBg: 'bg-error-50',
    iconText: '测',
    status: 'debugging' as const,
    agentCount: 2,
    updatedAt: '3天前',
  },
  {
    id: '5',
    name: 'Git历史分析',
    description: '深度分析 git 提交历史，生成项目演进报告和贡献者统计。',
    iconBg: 'bg-gray-100',
    iconText: 'G',
    status: 'published' as const,
    agentCount: 4,
    updatedAt: '2周前',
  },
]

// Skill Card Component
const SkillCard: React.FC<{
  name: string
  description: string
  iconBg: string
  iconText: string
  status: 'draft' | 'published' | 'debugging' | 'error' | 'info'
  agentCount: number
  updatedAt: string
  onClick?: () => void
  onUse?: () => void
  onEdit?: () => void
}> = ({ name, description, iconBg, iconText, status, agentCount, updatedAt, onClick, onUse, onEdit }) => {
  const statusLabel = {
    draft: '草稿',
    published: '已发布',
    debugging: '调试中',
    error: '错误',
    info: '进行中',
  }

  return (
    <div 
      className="w-full p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow cursor-pointer"
      onClick={onClick}
    >
      {/* Header Row */}
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-lg font-semibold ${iconBg}`}>
          {iconText}
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-sm font-semibold text-text-primary">{name}</span>
          <Badge variant={status}>{statusLabel[status]}</Badge>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-3 text-sm text-text-tertiary">
        <span>关联智能体: {agentCount}</span>
        <span>· {updatedAt}</span>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions */}
      <div className="flex gap-2">
        <Button variant="primary" size="sm" onClick={onUse}>使用</Button>
        <Button variant="secondary" size="sm" onClick={onEdit}>编辑</Button>
      </div>
    </div>
  )
}

// SkillListPage Component
const SkillListPage: React.FC = () => {
  const navigate = useNavigate()

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: 'Skill' }]}>
      <div className="p-8">
        {/* Title Row */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-xl font-semibold text-text-primary">我的 Skills</h1>
          <Button 
            variant="primary" 
            icon={<Plus className="w-4 h-4" />}
            onClick={() => navigate('/skills/create')}
          >
            创建 Skill
          </Button>
        </div>

        {/* Grid - 3 columns */}
        <div className="grid grid-cols-3 gap-6">
          {skillsData.map((skill) => (
            <SkillCard
              key={skill.id}
              name={skill.name}
              description={skill.description}
              iconBg={skill.iconBg}
              iconText={skill.iconText}
              status={skill.status}
              agentCount={skill.agentCount}
              updatedAt={skill.updatedAt}
              onUse={() => navigate('/chat')}
              onEdit={() => navigate('/skills/create')}
            />
          ))}
        </div>

        {/* Empty state if no skills */}
        {skillsData.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <Wand className="w-12 h-12 text-text-tertiary mb-4" />
            <p className="text-text-secondary mb-4">还没有创建任何 Skill</p>
            <Button 
              variant="primary" 
              icon={<Plus className="w-4 h-4" />}
              onClick={() => navigate('/skills/create')}
            >
              创建第一个 Skill
            </Button>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default SkillListPage