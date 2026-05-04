import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Filter, ArrowRight, Sparkles } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'

// Types
interface MarketCardProps {
  id: string
  name: string
  description: string
  icon: string
  author: string
  rating: number
  usageCount: number
  tags: string[]
}

// Mock data
const categories = [
  { name: '全部', count: 156, active: true },
  { name: '办公效率', count: 24 },
  { name: '研发工具', count: 38 },
  { name: '数据分析', count: 18 },
  { name: '客服支持', count: 15 },
  { name: '内容创作', count: 22 },
]

const sortTabs = [
  { name: '热门', active: true },
  { name: '最新' },
  { name: '评分' },
]

const agents: MarketCardProps[] = [
  { id: '1', name: '代码审查专家', description: '精准识别代码问题，支持多语言审查与优化建议', icon: '🤖', author: '李四', rating: 4.9, usageCount: 3200, tags: ['代码', '研发'] },
  { id: '2', name: '文档写作助手', description: '智能生成技术文档、API 说明、产品手册', icon: '📝', author: '张三', rating: 4.7, usageCount: 1800, tags: ['文档', '写作'] },
  { id: '3', name: '数据分析专家', description: '复杂数据分析、可视化图表生成、报告输出', icon: '📊', author: '王五', rating: 4.8, usageCount: 2400, tags: ['数据', '分析'] },
  { id: '4', name: '客服问答机器人', description: '24/7 智能客服，支持多轮对话与意图识别', icon: '💬', author: '赵六', rating: 4.6, usageCount: 5000, tags: ['客服', '对话'] },
  { id: '5', name: '翻译专家', description: '多语言翻译，支持技术文档、代码注释等专业内容', icon: '🌐', author: '孙七', rating: 4.5, usageCount: 1200, tags: ['翻译', '多语言'] },
  { id: '6', name: '项目管理助手', description: '任务分解、进度追踪、资源分配建议', icon: '📋', author: '周八', rating: 4.4, usageCount: 900, tags: ['项目', '管理'] },
]

// MarketCard Component
const MarketCard: React.FC<MarketCardProps & { onUse?: () => void }> = ({
  name,
  description,
  icon,
  author,
  rating,
  usageCount,
  tags,
  onUse,
}) => {
  return (
    <div className="p-5 bg-white rounded-xl border border-border-subtle flex flex-col gap-3">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="w-12 h-12 rounded-xl bg-brand-50 flex items-center justify-center text-2xl">
          {icon}
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-base font-semibold text-text-primary">{name}</span>
          <div className="flex items-center gap-2 text-xs text-text-tertiary">
            <span>{author}</span>
            <span>·</span>
            <span className="text-warning-500">⭐ {rating}</span>
            <span>·</span>
            <span>{usageCount}+ 使用</span>
          </div>
        </div>
      </div>

      {/* Description */}
      <p className="text-sm text-text-secondary line-clamp-2 leading-relaxed">
        {description}
      </p>

      {/* Tags */}
      <div className="flex items-center gap-2">
        {tags.map((tag) => (
          <span key={tag} className="px-2 py-0.5 bg-gray-100 rounded text-xs text-text-tertiary">
            {tag}
          </span>
        ))}
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

// CategoryItem Component
const CategoryItem: React.FC<{ name: string; count: number; active?: boolean; onClick?: () => void }> = ({
  name,
  count,
  active = false,
  onClick,
}) => {
  return (
    <button
      onClick={onClick}
      className={`flex items-center justify-between w-full px-2.5 py-2 rounded-md text-sm ${
        active ? 'bg-brand-50 text-brand-500' : 'hover:bg-gray-50 text-text-secondary'
      }`}
    >
      <span className={active ? 'font-semibold' : ''}>{name}</span>
      <span className={active ? 'text-brand-500' : 'text-text-tertiary'}>{count}</span>
    </button>
  )
}

// AgentMarketPage Component
const AgentMarketPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = React.useState('')
  const [activeCategory, setActiveCategory] = React.useState('全部')
  const [activeSort, setActiveSort] = React.useState('热门')

  const handleUseAgent = (id: string) => {
    navigate(`/market/agents/${id}`)
  }

  return (
    <Layout
      breadcrumb={[
        { label: '市场' },
        { label: '智能体市场' },
      ]}
    >
      <div className="p-8 flex flex-col gap-5 h-full">
        {/* Hero Banner */}
        <div 
          className="rounded-2xl p-7 flex items-center gap-6 bg-gradient-to-r from-brand-500 to-[#7C3AED]"
        >
          {/* Left Content */}
          <div className="flex-1 flex flex-col gap-3">
            <span className="text-xs font-semibold text-white/80">本周推荐</span>
            <h1 className="text-2xl font-bold text-white">
              Code Review Pro - AI 代码审查专家
            </h1>
            <p className="text-sm text-white/80">
              精准识别代码问题，评分超过 4.9 / 5，已服务 3200+ 开发者
            </p>
            <Button
              variant="secondary"
              onClick={() => handleUseAgent('1')}
              icon={<ArrowRight className="w-3.5 h-3.5" />}
              className="bg-white text-brand-500 hover:bg-gray-50"
            >
              立即使用
            </Button>
          </div>

          {/* Right Icon */}
          <div className="w-[200px] h-[140px] rounded-xl bg-white/[0.13] flex items-center justify-center">
            <Sparkles className="w-20 h-20 text-white/80" />
          </div>
        </div>

        {/* Body */}
        <div className="flex gap-6 h-full">
          {/* Sidebar - Categories */}
          <div className="w-[200px] flex flex-col gap-4 py-4">
            <span className="text-xs font-semibold text-text-tertiary px-1">分类</span>
            {categories.map((cat) => (
              <CategoryItem
                key={cat.name}
                name={cat.name}
                count={cat.count}
                active={activeCategory === cat.name}
                onClick={() => setActiveCategory(cat.name)}
              />
            ))}
          </div>

          {/* Main Content */}
          <div className="flex-1 flex flex-col gap-4">
            {/* Toolbar */}
            <div className="flex items-center gap-4">
              {/* Sort Tabs */}
              <div className="flex items-center gap-4">
                {sortTabs.map((tab) => (
                  <button
                    key={tab.name}
                    onClick={() => setActiveSort(tab.name)}
                    className={`px-0 py-1.5 text-sm ${
                      tab.active || activeSort === tab.name
                        ? 'font-semibold text-text-primary'
                        : 'text-text-tertiary'
                    }`}
                  >
                    {tab.name}
                  </button>
                ))}
              </div>

              {/* Search */}
              <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-white border border-border-subtle rounded-md">
                <Search className="w-4 h-4 text-text-tertiary" />
                <input
                  type="text"
                  placeholder="搜索智能体..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="flex-1 bg-transparent text-sm outline-none placeholder:text-text-tertiary"
                />
              </div>

              {/* Filter */}
              <Button variant="secondary" icon={<Filter className="w-4 h-4" />} iconOnly />
            </div>

            {/* Grid */}
            <div className="grid grid-cols-3 gap-4 overflow-auto">
              {agents.map((agent) => (
                <MarketCard
                  key={agent.id}
                  {...agent}
                  onUse={() => handleUseAgent(agent.id)}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentMarketPage