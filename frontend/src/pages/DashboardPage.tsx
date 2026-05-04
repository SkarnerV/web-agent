import React from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  MessageSquare, 
  Bot, 
  Wand, 
  Plug
} from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { AssetCard } from '../components/ui/AssetCard'

// DashboardPage - Home page with Layout wrapper
// Design spec from opus4.7.pen node 0HieJ
const DashboardPage: React.FC = () => {
  const navigate = useNavigate()

  // Get greeting based on current hour
  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return '早上好'
    if (hour < 18) return '下午好'
    return '晚上好'
  }

  // Get current date formatted
  const getCurrentDate = () => {
    const now = new Date()
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
  }

  // Quick action cards data
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

  // Recent agents data
  const recentAgents = [
    {
      id: '1',
      name: '需求文档助手',
      description: '帮助产品经理快速撰写和整理需求文档，支持多种模板格式',
      status: 'published' as const,
      toolCount: 5,
      collabCount: 2,
      updatedAt: '2d 前'
    },
    {
      id: '2',
      name: '代码审查专家',
      description: '专业的代码审查智能体，提供代码质量分析和改进建议',
      status: 'published' as const,
      toolCount: 3,
      collabCount: 1,
      updatedAt: '3d 前'
    },
    {
      id: '3',
      name: '数据分析助手',
      description: '支持多种数据源的分析和可视化，自动生成分析报告',
      status: 'published' as const,
      toolCount: 8,
      collabCount: 4,
      updatedAt: '5d 前'
    },
    {
      id: '4',
      name: '翻译专家',
      description: '高质量的中英互译智能体，支持专业术语和行业术语',
      status: 'published' as const,
      toolCount: 2,
      collabCount: 1,
      updatedAt: '1w 前'
    }
  ]

  // Stats data
  const stats = [
    { label: '智能体', value: '12' },
    { label: 'Skill', value: '5' },
    { label: 'MCP', value: '3' },
    { label: '知识库', value: '4' }
  ]

  // Featured marketplace items
  const featuredItems = [
    {
      icon: <Bot className="w-4 h-4 text-brand-500" />,
      iconBg: 'bg-brand-100',
      name: '会议纪要官',
      description: '自动整理会议内容'
    },
    {
      icon: <Wand className="w-4 h-4 text-purple-500" />,
      iconBg: 'bg-purple-50',
      name: '英文翻译 Skill',
      description: '高质量中英互译'
    },
    {
      icon: <Plug className="w-4 h-4 text-success-500" />,
      iconBg: 'bg-success-50',
      name: 'GitHub MCP',
      description: '代码仓库操作工具'
    }
  ]

  return (
    <Layout breadcrumb={[{ label: '首页' }]}>
      <div className="flex flex-col gap-6 p-8">
        {/* Greeting Section */}
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[26px] font-bold text-text-primary">
            {getGreeting()}, 张三 👋
          </h1>
          <p className="text-sm text-text-secondary">
            今天是 {getCurrentDate()}, 今天想做些什么?
          </p>
        </div>

        {/* Quick Actions Row - 4 cards */}
        <div className="flex gap-4">
          {quickActions.map((action, index) => (
            <button
              key={index}
              onClick={action.onClick}
              className="flex-1 bg-white rounded-xl border border-border-subtle flex flex-col gap-2 p-5 hover:shadow-md hover:border-border-strong transition-all"
            >
              {/* Icon */}
              <div className={`w-10 h-10 rounded-[10px] flex items-center justify-center ${action.iconBg}`}>
                {action.icon}
              </div>
              {/* Title */}
              <span className="text-[15px] font-semibold text-text-primary">
                {action.title}
              </span>
              {/* Description */}
              <span className="text-xs text-text-tertiary">
                {action.description}
              </span>
            </button>
          ))}
        </div>

        {/* Recent Agents Section */}
        <div className="flex flex-col gap-3">
          {/* Header */}
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

          {/* Agents Row */}
          <div className="flex gap-4">
            {recentAgents.map((agent) => (
              <div key={agent.id} className="flex-1">
                <AssetCard
                  id={agent.id}
                  name={agent.name}
                  description={agent.description}
                  iconBg="bg-brand-50"
                  iconText={agent.name[0]}
                  status={agent.status}
                  toolCount={agent.toolCount}
                  collabCount={agent.collabCount}
                  updatedAt={agent.updatedAt}
                  onUse={() => navigate('/chat')}
                  onEdit={() => navigate(`/agents/edit/${agent.id}`)}
                />
              </div>
            ))}
          </div>
        </div>

        {/* Bottom Section - Stats + Featured */}
        <div className="flex gap-4">
          {/* Stats Card */}
          <div className="flex-1 bg-white rounded-xl border border-border-subtle flex flex-col gap-4 p-6">
            <span className="text-[15px] font-semibold text-text-primary">
              我的资产统计
            </span>
            
            {/* Stats Grid */}
            <div className="flex gap-6">
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
          </div>

          {/* Featured Card */}
          <div className="flex-1 bg-white rounded-xl border border-border-subtle flex flex-col gap-3 p-6">
            {/* Header */}
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

            {/* Featured Items List */}
            <div className="flex flex-col gap-2.5">
              {featuredItems.map((item) => (
                <div 
                  key={item.name}
                  className="bg-gray-50 rounded-lg flex items-center gap-2.5 p-2.5 hover:bg-gray-100 transition-colors cursor-pointer"
                >
                  {/* Icon */}
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${item.iconBg}`}>
                    {item.icon}
                  </div>
                  {/* Text */}
                  <div className="flex-1 flex flex-col gap-0.5">
                    <span className="text-[13px] font-semibold text-text-primary">
                      {item.name}
                    </span>
                    <span className="text-[11px] text-text-tertiary">
                      {item.description}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}

export default DashboardPage