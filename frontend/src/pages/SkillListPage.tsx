import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Wand, Search, Code, FileText, Database, GitBranch, Shield } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// ── Icon mapping ──

const iconMap: Record<string, React.FC<{ className?: string }>> = {
  'code-review': Code,
  'doc-gen': FileText,
  'api-gen': Code,
  'test-coverage': Shield,
  'git-history': GitBranch,
  'data-analyze': Database,
}

// ── Mock data ──

interface SkillData {
  id: string
  name: string
  description: string
  icon: string
  iconBg: string
  iconColor: string
  type: string
  status: 'published' | 'draft'
  updatedAt: string
}

const skillsData: SkillData[] = [
  {
    id: '1', name: '代码检视专家', type: 'Prompt 型',
    description: '启动一个专业的代码检视子代理，按照预定义的检视方向对代码进行深度分析，输出JSON格式的详细检视报告。',
    icon: 'code-review', iconBg: 'bg-brand-50', iconColor: 'text-brand-500',
    status: 'published', updatedAt: '2天前',
  },
  {
    id: '2', name: '开发反思', type: 'Chain 型',
    description: '生成一个反思 markdown 文件，捕获 bug fix 过程中发生了什么、尝试了什么、以及关键收获。',
    icon: 'doc-gen', iconBg: 'bg-warning-50', iconColor: 'text-warning-500',
    status: 'draft', updatedAt: '5天前',
  },
  {
    id: '3', name: 'API文档生成', type: 'Prompt 型',
    description: '自动从代码注释和类型定义生成完整的 API 文档，支持多种格式输出。',
    icon: 'api-gen', iconBg: 'bg-success-50', iconColor: 'text-success-500',
    status: 'published', updatedAt: '1周前',
  },
  {
    id: '4', name: '测试覆盖率分析', type: 'Chain 型',
    description: '分析项目测试覆盖率，识别缺失的测试场景并生成测试建议。',
    icon: 'test-coverage', iconBg: 'bg-error-50', iconColor: 'text-error-500',
    status: 'published', updatedAt: '3天前',
  },
  {
    id: '5', name: 'Git历史分析', type: 'Prompt 型',
    description: '深度分析 git 提交历史，生成项目演进报告和贡献者统计。',
    icon: 'git-history', iconBg: 'bg-gray-100', iconColor: 'text-gray-600',
    status: 'published', updatedAt: '2周前',
  },
]

// ── SkillCard ──

const SkillCard: React.FC<{
  skill: SkillData
  onUse?: () => void
  onEdit?: () => void
  onTest?: () => void
  onCopy?: () => void
}> = ({ skill, onUse, onEdit, onTest, onCopy }) => {
  const IconComponent = iconMap[skill.icon] ?? Wand

  return (
    <div className="p-5 bg-white rounded-lg border border-border-subtle flex flex-col gap-3 hover:shadow-md transition-shadow">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${skill.iconBg}`}>
          <IconComponent className={`w-5 h-5 ${skill.iconColor}`} />
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-sm font-semibold text-text-primary">{skill.name}</span>
          <Badge variant={skill.status === 'published' ? 'published' : 'draft'}>
            {skill.status === 'published' ? '已发布' : '草稿'}
          </Badge>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {skill.description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-2 text-xs text-text-tertiary">
        <span>{skill.type}</span>
        <span>·</span>
        <span>{skill.updatedAt}</span>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions — text-link style */}
      <div className="flex items-center gap-3 text-[13px] text-brand-500">
        <button onClick={onUse} className="hover:underline">使用</button>
        <button onClick={onEdit} className="hover:underline">编辑</button>
        <button onClick={onTest} className="hover:underline">测试</button>
        <button onClick={onCopy} className="hover:underline">复制</button>
      </div>
    </div>
  )
}

// ── Page ──

const SkillListPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<'all' | 'published' | 'draft'>('all')
  const [searchQuery, setSearchQuery] = useState('')

  const filtered = skillsData
    .filter((s) => activeTab === 'all' || s.status === activeTab)
    .filter((s) => s.name.toLowerCase().includes(searchQuery.toLowerCase()))

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: 'Skill' }]}>
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        {/* Title Row */}
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold text-text-primary">我的 Skills</h1>
          <Button
            variant="primary"
            icon={<Plus className="w-4 h-4" />}
            onClick={() => navigate('/skills/create')}
          >
            创建 Skill
          </Button>
        </div>

        {/* Tabs + Search */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1 flex-1">
            {[
              { key: 'all', label: '全部' },
              { key: 'published', label: '已发布' },
              { key: 'draft', label: '草稿' },
            ].map((tab) => {
              const count = skillsData.filter((s) =>
                tab.key === 'all' ? true : s.status === tab.key,
              ).length
              return (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key as typeof activeTab)}
                  className={`px-3.5 py-2 rounded-md text-[13px] font-medium transition-colors ${
                    activeTab === tab.key
                      ? 'bg-brand-50 text-brand-500 font-semibold'
                      : 'text-text-secondary hover:text-text-primary'
                  }`}
                >
                  {tab.label} {count}
                </button>
              )
            })}
          </div>
          {/* Search */}
          <div className="flex items-center gap-2 px-3 py-2 bg-white border border-border-subtle rounded-md w-[220px]">
            <Search className="w-3.5 h-3.5 text-text-tertiary flex-shrink-0" />
            <input
              type="text"
              placeholder="搜索 Skill..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 bg-transparent text-[13px] outline-none placeholder:text-text-tertiary"
            />
          </div>
        </div>

        {/* Grid */}
        <div className="grid grid-cols-3 gap-4">
          {filtered.map((skill) => (
            <SkillCard
              key={skill.id}
              skill={skill}
              onUse={() => navigate('/chat')}
              onEdit={() => navigate('/skills/create')}
              onTest={() => navigate('/chat')}
              onCopy={() => navigate('/skills/create')}
            />
          ))}
          {/* Create new placeholder */}
          <button
            onClick={() => navigate('/skills/create')}
            className="p-5 bg-white rounded-lg border-2 border-dashed border-border-subtle flex flex-col items-center justify-center gap-2 hover:border-brand-500 hover:bg-brand-50/30 transition-colors min-h-[200px]"
          >
            <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center">
              <Plus className="w-5 h-5 text-brand-500" />
            </div>
            <span className="text-sm font-medium text-brand-500">创建新 Skill</span>
          </button>
        </div>

        {/* Empty state */}
        {filtered.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <Wand className="w-12 h-12 text-text-tertiary mb-4" />
            <p className="text-text-secondary mb-4">没有匹配的 Skill</p>
            <Button variant="primary" icon={<Plus className="w-4 h-4" />} onClick={() => navigate('/skills/create')}>
              创建第一个 Skill
            </Button>
          </div>
        )}
      </div>
    </Layout>
  )
}

export default SkillListPage
