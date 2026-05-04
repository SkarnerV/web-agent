import React from 'react'
import { useNavigate } from 'react-router-dom'

import { Wand, ArrowRight } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

// Types
interface SkillCardProps {
  id: string
  name: string
  description: string
  category: string
  usageCount: number
  rating: number
}

// Mock data
const categoryTabs = [
  { name: '全部', active: true },
  { name: '代码生成' },
  { name: '数据处理' },
  { name: '文件操作' },
  { name: '网络请求' },
]

const skills: SkillCardProps[] = [
  { id: '1', name: '代码格式化', description: '自动格式化代码，支持多种语言风格', category: '代码生成', usageCount: 1200, rating: 4.8 },
  { id: '2', name: 'JSON 解析', description: '解析和处理 JSON 数据结构', category: '数据处理', usageCount: 800, rating: 4.7 },
  { id: '3', name: '文件读取', description: '读取各类文件内容并提取关键信息', category: '文件操作', usageCount: 1500, rating: 4.9 },
  { id: '4', name: 'API 调用', description: '发起 HTTP 请求并处理响应数据', category: '网络请求', usageCount: 2000, rating: 4.6 },
  { id: '5', name: '代码补全', description: '智能代码补全和自动生成建议', category: '代码生成', usageCount: 3000, rating: 4.8 },
  { id: '6', name: '表格处理', description: '处理 CSV、Excel 等表格数据', category: '数据处理', usageCount: 600, rating: 4.5 },
  { id: '7', name: '图像处理', description: '图像裁剪、压缩、格式转换', category: '文件操作', usageCount: 400, rating: 4.4 },
  { id: '8', name: '数据库查询', description: '执行数据库查询并返回结果', category: '数据处理', usageCount: 900, rating: 4.7 },
  { id: '9', name: 'WebSocket', description: 'WebSocket 连接管理和消息处理', category: '网络请求', usageCount: 300, rating: 4.3 },
]

// SkillCard Component
const SkillCard: React.FC<SkillCardProps & { onUse?: () => void }> = ({
  name,
  description,
  category,
  usageCount,
  rating,
  onUse,
}) => {
  return (
    <div className="p-5 bg-white rounded-xl border border-border-subtle flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg bg-purple-50 flex items-center justify-center">
          <Wand className="w-5 h-5 text-purple-500" />
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-sm font-semibold text-text-primary">{name}</span>
          <span className="text-xs text-text-tertiary">{category}</span>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Meta */}
      <div className="flex items-center gap-2 text-xs text-text-tertiary">
        <span className="text-warning-500">⭐ {rating}</span>
        <span>·</span>
        <span>{usageCount}+ 使用</span>
      </div>

      {/* Divider */}
      <div className="h-px bg-border-subtle" />

      {/* Actions */}
      <Button variant="primary" onClick={onUse} icon={<ArrowRight className="w-4 h-4" />}>
        使用
      </Button>
    </div>
  )
}

// CategoryTab Component
const CategoryTab: React.FC<{ name: string; active?: boolean; onClick?: () => void }> = ({
  name,
  active = false,
  onClick,
}) => {
  return (
    <button
      onClick={onClick}
      className={`px-3.5 py-1.5 rounded-md text-sm ${
        active
          ? 'bg-text-primary text-white'
          : 'bg-white text-text-secondary border border-border-subtle hover:bg-gray-50'
      }`}
    >
      {name}
    </button>
  )
}

// SkillMarketPage Component
const SkillMarketPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeCategory, setActiveCategory] = React.useState('全部')

  const handleUseSkill = (_id: string) => {
    // Navigate to skill detail or chat to use the skill
    navigate('/chat')
  }

  return (
    <Layout
      breadcrumb={[
        { label: '市场' },
        { label: 'Skill 市场' },
      ]}
    >
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        {/* Hero Banner */}
        <div className="rounded-xl bg-[#8B5CF6] p-6 flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold text-white">发现可复用的 Skill</h1>
          <p className="text-[13px] text-white/80">
            为你的智能体添加即插即用的原子能力 · 收录 {skills.length} 个社区 Skill
          </p>
        </div>

        {/* Category Tabs */}
        <div className="flex items-center gap-2">
          {categoryTabs.map((tab) => (
            <CategoryTab
              key={tab.name}
              name={tab.name}
              active={activeCategory === tab.name}
              onClick={() => setActiveCategory(tab.name)}
            />
          ))}
        </div>

        {/* Grid */}
        <div className="grid grid-cols-4 gap-4">
          {skills
            .filter((skill) => activeCategory === '全部' || skill.category === activeCategory)
            .map((skill) => (
              <SkillCard
                key={skill.id}
                {...skill}
                onUse={() => handleUseSkill(skill.id)}
              />
            ))}
        </div>
      </div>
    </Layout>
  )
}

export default SkillMarketPage