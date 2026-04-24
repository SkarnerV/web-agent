import React from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Heart, ArrowRight, Bot, Users, Star, ChevronRight } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'

// Types
interface TabItem {
  id: string
  label: string
  active: boolean
}

// Mock data
const agentData = {
  id: '1',
  name: '代码审查专家',
  icon: '🤖',
  version: 'v2.1.0',
  author: '李四 · AI 部门',
  publishedAt: '2天前',
  rating: 4.9,
  reviewCount: 238,
  usageCount: 3200,
  favoriteCount: 156,
  description: '代码审查专家是一款面向专业开发者的 AI 助手，支持多种主流语言（Python / TypeScript / Go / Java）的代码审查与优化建议。该智能体集成了静态分析、安全扫描、性能检测三大能力，能够快速识别潜在问题并给出修改方案。',
  scenes: [
    '提交 Pull Request 后的自动审查',
    '代码重构前的风险评估',
    '新人代码 Review 培训辅助',
  ],
  tags: ['代码审查', '静态分析', '安全扫描', '性能检测', 'Python', 'TypeScript'],
  stats: [
    { label: '使用次数', value: '3,200+', icon: Users },
    { label: '收藏数', value: '156', icon: Heart },
    { label: '好评率', value: '98%', icon: Star },
  ],
  tools: [
    { name: '代码解析器', description: '解析代码结构，提取关键信息', icon: '🔧' },
    { name: '安全扫描器', description: '检测潜在安全漏洞', icon: '🔒' },
    { name: '性能分析器', description: '分析代码性能瓶颈', icon: '⚡' },
    { name: '规则引擎', description: '应用自定义审查规则', icon: '📋' },
    { name: '报告生成器', description: '生成审查报告', icon: '📊' },
  ],
  reviews: [
    { author: '开发者A', avatar: 'A', rating: 5, content: '非常好用，帮我发现了不少代码问题！', date: '1天前' },
    { author: '开发者B', avatar: 'B', rating: 5, content: '审查速度快，建议准确，推荐使用。', date: '3天前' },
    { author: '开发者C', avatar: 'C', rating: 4, content: '功能全面，但对某些边缘情况处理不够完善。', date: '5天前' },
  ],
  versions: [
    { version: 'v2.1.0', date: '2024-01-15', changes: '新增 Go 语言支持' },
    { version: 'v2.0.0', date: '2024-01-01', changes: '重构核心引擎，性能提升 50%' },
    { version: 'v1.5.0', date: '2023-12-15', changes: '新增安全扫描功能' },
  ],
}

// StatCard Component
const StatCard: React.FC<{ label: string; value: string; icon: React.ReactNode }> = ({
  label,
  value,
  icon,
}) => {
  return (
    <div className="flex items-center gap-3">
      <div className="w-10 h-10 rounded-lg bg-brand-50 flex items-center justify-center text-brand-500">
        {icon}
      </div>
      <div className="flex flex-col">
        <span className="text-xs text-text-tertiary">{label}</span>
        <span className="text-base font-semibold text-text-primary">{value}</span>
      </div>
    </div>
  )
}

// ToolCard Component
const ToolCard: React.FC<{ name: string; description: string; icon: string }> = ({
  name,
  description,
  icon,
}) => {
  return (
    <div className="flex items-center gap-2 py-2">
      <span className="text-lg">{icon}</span>
      <div className="flex-1 flex flex-col">
        <span className="text-sm font-medium text-text-primary">{name}</span>
        <span className="text-xs text-text-tertiary">{description}</span>
      </div>
    </div>
  )
}

// ReviewCard Component
const ReviewCard: React.FC<{ author: string; avatar: string; rating: number; content: string; date: string }> = ({
  author,
  avatar,
  rating,
  content,
  date,
}) => {
  return (
    <div className="flex gap-3 py-3 border-b border-border-subtle last:border-0">
      <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-sm font-medium text-text-primary">
        {avatar}
      </div>
      <div className="flex-1 flex flex-col gap-1">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-text-primary">{author}</span>
          <div className="flex items-center gap-0.5">
            {[...Array(5)].map((_, i) => (
              <Star
                key={i}
                className={`w-3 h-3 ${i < rating ? 'fill-warning-500 text-warning-500' : 'text-gray-300'}`}
              />
            ))}
          </div>
          <span className="text-xs text-text-tertiary">{date}</span>
        </div>
        <p className="text-sm text-text-secondary">{content}</p>
      </div>
    </div>
  )
}

// VersionRow Component
const VersionRow: React.FC<{ version: string; date: string; changes: string }> = ({
  version,
  date,
  changes,
}) => {
  return (
    <div className="flex items-center gap-4 py-3 border-b border-border-subtle last:border-0">
      <span className="text-sm font-medium text-brand-500">{version}</span>
      <span className="text-xs text-text-tertiary">{date}</span>
      <span className="text-sm text-text-secondary">{changes}</span>
    </div>
  )
}

// AgentDetailPage Component
const AgentDetailPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = React.useState('intro')
  const [isFavorite, setIsFavorite] = React.useState(false)

  const tabs: TabItem[] = [
    { id: 'intro', label: '简介', active: activeTab === 'intro' },
    { id: 'tools', label: '工具能力', active: activeTab === 'tools' },
    { id: 'versions', label: '版本历史', active: activeTab === 'versions' },
    { id: 'reviews', label: `评价 (${agentData.reviewCount})`, active: activeTab === 'reviews' },
  ]

  const handleBack = () => {
    navigate('/market/agents')
  }

  const handleUse = () => {
    // Navigate to chat page to start conversation with this agent
    navigate('/chat')
  }

  const handleFavorite = () => {
    setIsFavorite(!isFavorite)
  }

  return (
    <Layout
      breadcrumb={[
        { label: '智能体市场', path: '/market/agents' },
        { label: agentData.name },
      ]}
    >
      <div className="p-8 flex flex-col gap-5 h-full overflow-auto">
        {/* Back Button */}
        <button
          onClick={handleBack}
          className="flex items-center gap-1 text-sm text-text-secondary hover:text-text-primary"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>返回市场</span>
        </button>

        {/* Header Card */}
        <div className="p-7 rounded-2xl bg-white border border-border-subtle flex items-center gap-5">
          {/* Icon */}
          <div className="w-20 h-20 rounded-2xl bg-brand-50 flex items-center justify-center">
            <Bot className="w-10 h-10 text-brand-500" />
          </div>

          {/* Info */}
          <div className="flex-1 flex flex-col gap-1.5">
            <div className="flex items-center gap-2.5">
              <h1 className="text-xl font-bold text-text-primary">{agentData.name}</h1>
              <Badge variant="info">{agentData.version}</Badge>
            </div>
            <div className="flex items-center gap-2.5 text-xs text-text-tertiary">
              <div className="w-5 h-5 rounded-full bg-gray-300 flex items-center justify-center text-xs">
                {agentData.author[0]}
              </div>
              <span>{agentData.author}</span>
              <span>·</span>
              <span>发布于 {agentData.publishedAt}</span>
              <span>·</span>
              <span className="text-warning-500">⭐ {agentData.rating} ({agentData.reviewCount} 评价)</span>
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-2.5">
            <Button
              variant="secondary"
              onClick={handleFavorite}
              icon={<Heart className={`w-3.5 h-3.5 ${isFavorite ? 'fill-error-500 text-error-500' : ''}`} />}
            >
              收藏
            </Button>
            <Button
              variant="primary"
              onClick={handleUse}
              icon={<ArrowRight className="w-3.5 h-3.5" />}
            >
              使用
            </Button>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex items-center gap-1 border-b border-border-subtle">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2.5 text-sm ${
                tab.active
                  ? 'font-semibold text-brand-500 border-b-2 border-brand-500'
                  : 'text-text-secondary hover:text-text-primary'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="flex gap-5 h-full">
          {/* Left Column */}
          <div className="flex-1 p-6 rounded-xl bg-white border border-border-subtle flex flex-col gap-4">
            {activeTab === 'intro' && (
              <>
                <h2 className="text-base font-bold text-text-primary">产品简介</h2>
                <p className="text-sm text-text-secondary leading-relaxed">{agentData.description}</p>
                
                <h3 className="text-sm font-bold text-text-primary">使用场景</h3>
                <div className="flex flex-col gap-2">
                  {agentData.scenes.map((scene, i) => (
                    <p key={i} className="text-sm text-text-secondary">• {scene}</p>
                  ))}
                </div>
              </>
            )}

            {activeTab === 'tools' && (
              <>
                <h2 className="text-base font-bold text-text-primary">内置工具 ({agentData.tools.length})</h2>
                <div className="flex flex-col gap-2">
                  {agentData.tools.map((tool) => (
                    <ToolCard key={tool.name} {...tool} />
                  ))}
                </div>
              </>
            )}

            {activeTab === 'versions' && (
              <>
                <h2 className="text-base font-bold text-text-primary">版本历史</h2>
                <div className="flex flex-col">
                  {agentData.versions.map((v) => (
                    <VersionRow key={v.version} {...v} />
                  ))}
                </div>
              </>
            )}

            {activeTab === 'reviews' && (
              <>
                <h2 className="text-base font-bold text-text-primary">用户评价</h2>
                <div className="flex flex-col">
                  {agentData.reviews.map((review) => (
                    <ReviewCard key={review.author} {...review} />
                  ))}
                </div>
              </>
            )}
          </div>

          {/* Right Column */}
          <div className="w-[360px] flex flex-col gap-4">
            {/* Stats Box */}
            <div className="p-5 rounded-xl bg-white border border-border-subtle flex flex-col gap-3">
              <h3 className="text-sm font-semibold text-text-primary">数据统计</h3>
              <div className="flex items-center gap-4">
                {agentData.stats.map((stat) => (
                  <StatCard key={stat.label} {...stat} icon={<stat.icon className="w-5 h-5" />} />
                ))}
              </div>
            </div>

            {/* Tags Box */}
            <div className="p-5 rounded-xl bg-white border border-border-subtle flex flex-col gap-2.5">
              <h3 className="text-sm font-semibold text-text-primary">标签</h3>
              <div className="flex items-center gap-1.5 flex-wrap">
                {agentData.tags.map((tag) => (
                  <span key={tag} className="px-2 py-1 bg-gray-100 rounded text-xs text-text-secondary">
                    {tag}
                  </span>
                ))}
              </div>
            </div>

            {/* Tools Preview */}
            <div className="p-5 rounded-xl bg-white border border-border-subtle flex flex-col gap-2.5">
              <h3 className="text-sm font-semibold text-text-primary">内置工具 ({agentData.tools.length})</h3>
              {agentData.tools.slice(0, 3).map((tool) => (
                <div key={tool.name} className="flex items-center gap-2">
                  <span>{tool.icon}</span>
                  <span className="text-sm text-text-primary">{tool.name}</span>
                </div>
              ))}
              <button className="flex items-center gap-1 text-sm text-brand-500 hover:text-brand-600">
                <span>查看全部</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default AgentDetailPage