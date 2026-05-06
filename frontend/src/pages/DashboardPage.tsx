import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  MessageSquare, 
  Bot, 
  Wand, 
  Plug,
  Loader2
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { AssetCard } from '../components/ui/AssetCard'
import { listAgents } from '../api/agent'
import { listSkills } from '../api/skill'
import { listMcps } from '../api/mcp'
import { listKnowledgeBases } from '../api/knowledge'
import { getFeatured } from '../api/market'
import type { AgentSummaryVO, MarketItemVO } from '../api/types'

const DashboardPage: React.FC = () => {
  const navigate = useNavigate()

  const [recentAgents, setRecentAgents] = useState<AgentSummaryVO[]>([])
  const [stats, setStats] = useState<{ label: string; value: string }[]>([
    { label: '智能体', value: '-' },
    { label: 'Skill', value: '-' },
    { label: 'MCP', value: '-' },
    { label: '知识库', value: '-' },
  ])
  const [featuredItems, setFeaturedItems] = useState<MarketItemVO[]>([])
  const [loadingAgents, setLoadingAgents] = useState(true)
  const [loadingStats, setLoadingStats] = useState(true)
  const [loadingFeatured, setLoadingFeatured] = useState(true)

  useEffect(() => {
    listAgents({ sort_by: 'updated_at', sort_order: 'desc', page_size: 4 })
      .then(res => setRecentAgents(res.data))
      .catch(() => {})
      .finally(() => setLoadingAgents(false))

    Promise.all([
      listAgents({ page_size: 1 }),
      listSkills({ page_size: 1 }),
      listMcps({ page_size: 1 }),
      listKnowledgeBases({ page_size: 1 }),
    ])
      .then(([agents, skills, mcps, kbs]) => {
        setStats([
          { label: '智能体', value: String(agents.total) },
          { label: 'Skill', value: String(skills.total) },
          { label: 'MCP', value: String(mcps.total) },
          { label: '知识库', value: String(kbs.total) },
        ])
      })
      .catch(() => {})
      .finally(() => setLoadingStats(false))

    getFeatured()
      .then(res => setFeaturedItems(res.data))
      .catch(() => {})
      .finally(() => setLoadingFeatured(false))
  }, [])

  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return '早上好'
    if (hour < 18) return '下午好'
    return '晚上好'
  }

  const getCurrentDate = () => {
    const now = new Date()
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
  }

  const quickActions = [
    {
      icon: <MessageSquare className="w-5 h-5 text-brand-500" />,
      iconBg: 'bg-brand-50',
      title: '开始对话',
      description: '与智能体快速交流',
      onClick: () => navigate('/chat')
    },
    {
      icon: <Bot className="w-5 h-5 text-sky-500" />,
      iconBg: 'bg-sky-50',
      title: '创建智能体',
      description: '配置专属 AI 助手',
      onClick: () => navigate('/agents/create')
    },
    {
      icon: <Wand className="w-5 h-5 text-purple-500" />,
      iconBg: 'bg-purple-50',
      title: '创建 Skill',
      description: '封装多步骤作业能力',
      onClick: () => navigate('/skills/create')
    },
    {
      icon: <Plug className="w-5 h-5 text-success-500" />,
      iconBg: 'bg-success-50',
      title: '添加 MCP',
      description: '接入外部工具服务器',
      onClick: () => navigate('/mcp/add')
    }
  ]

  const getFeaturedIcon = (item: MarketItemVO) => {
    switch (item.assetType) {
      case 'AGENT': return <Bot className="w-4 h-4 text-brand-500" />
      case 'SKILL': return <Wand className="w-4 h-4 text-purple-500" />
      case 'MCP': return <Plug className="w-4 h-4 text-success-500" />
      default: return <Bot className="w-4 h-4 text-brand-500" />
    }
  }

  const getFeaturedIconBg = (item: MarketItemVO) => {
    switch (item.assetType) {
      case 'AGENT': return 'bg-brand-100'
      case 'SKILL': return 'bg-purple-50'
      case 'MCP': return 'bg-success-50'
      default: return 'bg-brand-100'
    }
  }

  return (
    <Layout breadcrumb={[{ label: '首页' }]}>
      <div className="flex flex-col gap-6 p-8">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[26px] font-bold text-text-primary">
            {getGreeting()}, 张三 👋
          </h1>
          <p className="text-sm text-text-secondary">
            今天是 {getCurrentDate()}, 今天想做些什么?
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 2xl:grid-cols-4">
          {quickActions.map((action, index) => (
            <button
              key={index}
              onClick={action.onClick}
              className="min-w-0 bg-white rounded-xl border border-border-subtle flex flex-col gap-2 p-5 text-left hover:shadow-md hover:border-border-strong transition-all"
            >
              <div className={`w-10 h-10 rounded-[10px] flex items-center justify-center ${action.iconBg}`}>
                {action.icon}
              </div>
              <span className="text-[15px] font-semibold text-text-primary">
                {action.title}
              </span>
              <span className="text-xs text-text-tertiary">
                {action.description}
              </span>
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-3">
          <div className="flex items-center">
            <span className="flex-1 text-lg font-semibold text-text-primary">
              最近使用的智能体
            </span>
            <button 
              onClick={() => navigate('/agents')}
              className="text-[13px] font-medium text-brand-500 hover:text-brand-600"
            >
              查看全部 →
            </button>
          </div>

          {loadingAgents ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="w-6 h-6 text-brand-500 animate-spin" />
            </div>
          ) : recentAgents.length === 0 ? (
            <div className="flex items-center justify-center py-8 text-sm text-text-tertiary">
              暂无智能体
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 xl:grid-cols-2 2xl:grid-cols-4">
              {recentAgents.map((agent) => (
                <div key={agent.id} className="min-w-0">
                  <AssetCard
                    id={agent.id}
                    name={agent.name}
                    description={agent.description || ''}
                    iconType="agent"
                    status={agent.status === 'PUBLISHED' ? 'published' : agent.status === 'DRAFT' ? 'draft' : undefined}
                    toolCount={0}
                    collabCount={0}
                    updatedAt={agent.updatedAt ? new Date(agent.updatedAt).toLocaleDateString() : ''}
                    onUse={() => navigate('/chat')}
                    onEdit={() => navigate(`/agents/edit/${agent.id}`)}
                  />
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 gap-4 2xl:grid-cols-2">
          <div className="bg-white rounded-xl border border-border-subtle flex flex-col gap-4 p-6">
            <span className="text-[15px] font-semibold text-text-primary">
              我的资产统计
            </span>
            
            {loadingStats ? (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="w-5 h-5 text-brand-500 animate-spin" />
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-6 xl:grid-cols-4">
                {stats.map((stat) => (
                  <div key={stat.label} className="flex-1 flex flex-col gap-1">
                    <span className="text-xs text-text-tertiary">
                      {stat.label}
                    </span>
                    <span className="text-[28px] font-bold text-text-primary">
                      {stat.value}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="bg-white rounded-xl border border-border-subtle flex flex-col gap-3 p-6">
            <div className="flex items-center">
              <span className="flex-1 text-[15px] font-semibold text-text-primary">
                市场精选
              </span>
              <button 
                onClick={() => navigate('/market/agents')}
                className="text-xs text-brand-500 hover:text-brand-600"
              >
                更多 →
              </button>
            </div>

            {loadingFeatured ? (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="w-5 h-5 text-brand-500 animate-spin" />
              </div>
            ) : featuredItems.length === 0 ? (
              <div className="flex items-center justify-center py-4 text-sm text-text-tertiary">
                暂无精选
              </div>
            ) : (
              <div className="flex flex-col gap-2.5">
                {featuredItems.slice(0, 3).map((item) => (
                  <div 
                    key={item.id}
                    className="bg-gray-50 rounded-lg flex items-center gap-2.5 p-2.5 hover:bg-gray-100 transition-colors cursor-pointer"
                  >
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${getFeaturedIconBg(item)}`}>
                      {getFeaturedIcon(item)}
                    </div>
                    <div className="flex-1 flex flex-col gap-0.5">
                      <span className="text-[13px] font-semibold text-text-primary">
                        {item.assetType === 'AGENT' ? '智能体' : item.assetType === 'SKILL' ? 'Skill' : 'MCP'}
                      </span>
                      <span className="text-[11px] text-text-tertiary">
                        {item.category || `${item.useCount} 次使用`}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default DashboardPage
