import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Layout } from '../components/layout'
import { Bot, Wand2, Plug, ArrowRight } from 'lucide-react'

// Types for wizard cards
interface WizardCardType {
  id: string
  title: string
  description: string
  icon: React.ReactNode
  iconBg: string
  iconColor: string
  buttonBg: string
  features: string[]
  path: string
}

// Wizard card data based on design spec
const wizardCards: WizardCardType[] = [
  {
    id: 'agent',
    title: 'Agent 智能体',
    description: '面向特定场景的完整 AI 助手',
    icon: <Bot className="w-7 h-7" />,
    iconBg: 'bg-brand-50',
    iconColor: 'text-brand-500',
    buttonBg: 'bg-brand-500 hover:bg-brand-600',
    features: [
      '自定义角色和提示词',
      '集成工具和 MCP',
      '多智能体协作',
    ],
    path: '/agents/create',
  },
  {
    id: 'skill',
    title: 'Skill 技能',
    description: '可复用的自动化工作流单元',
    icon: <Wand2 className="w-7 h-7" />,
    iconBg: 'bg-purple-50',
    iconColor: 'text-purple-500',
    buttonBg: 'bg-purple-500 hover:bg-purple-600',
    features: [
      'YAML / Markdown 定义',
      '可被 Agent 调用',
      '支持市场共享',
    ],
    path: '/skills/create',
  },
  {
    id: 'mcp',
    title: 'MCP 服务器',
    description: '通过标准协议接入外部工具',
    icon: <Plug className="w-7 h-7" />,
    iconBg: 'bg-success-50',
    iconColor: 'text-success-500',
    buttonBg: 'bg-success-500 hover:bg-green-600',
    features: [
      'SSE / HTTP 协议',
      '批量提供多个工具',
      '统一权限控制',
    ],
    path: '/mcp/add',
  },
]

// WizardCard component
const WizardCard: React.FC<{
  card: WizardCardType
  onClick: () => void
}> = ({ card, onClick }) => {
  return (
    <div
      className="w-[280px] p-7 bg-white rounded-2xl border border-border-subtle flex flex-col gap-4 cursor-pointer transition-all hover:shadow-lg hover:border-border-strong"
      onClick={onClick}
    >
      {/* Icon */}
      <div className={`w-14 h-14 rounded-[14px] ${card.iconBg} flex items-center justify-center ${card.iconColor}`}>
        {card.icon}
      </div>

      {/* Title section */}
      <div className="flex flex-col gap-1.5">
        <span className="text-lg font-bold text-text-primary">{card.title}</span>
        <span className="text-xs text-text-secondary leading-relaxed">{card.description}</span>
      </div>

      {/* Features list */}
      <div className="flex flex-col gap-1.5">
        {card.features.map((feature, index) => (
          <span key={index} className="text-xs text-text-secondary">• {feature}</span>
        ))}
      </div>

      {/* Button */}
      <button
        className={`w-full py-2.5 px-3.5 rounded-lg ${card.buttonBg} flex items-center justify-center gap-1.5 text-white text-[13px] font-semibold transition-colors`}
        onClick={(e) => {
          e.stopPropagation()
          onClick()
        }}
      >
        开始创建
        <ArrowRight className="w-3.5 h-3.5" />
      </button>
    </div>
  )
}

// CreationWizardPage component
const CreationWizardPage: React.FC = () => {
  const navigate = useNavigate()

  const handleCardClick = (path: string) => {
    navigate(path)
  }

  return (
    <Layout breadcrumb={[
      { label: '创建', path: '/create' },
      { label: '选择类型' },
    ]}>
      <div className="flex flex-col gap-8 px-8 py-16">
        {/* Title section */}
        <div className="flex flex-col gap-2 items-center">
          <h1 className="text-[28px] font-bold text-text-primary">选择要创建的类型</h1>
          <p className="text-sm text-text-secondary">你可以创建以下三种资产，组合使用构建自己的 AI 助手</p>
        </div>

        {/* Cards row */}
        <div className="flex gap-5 py-4 justify-center">
          {wizardCards.map((card) => (
            <WizardCard
              key={card.id}
              card={card}
              onClick={() => handleCardClick(card.path)}
            />
          ))}
        </div>
      </div>
    </Layout>
  )
}

export default CreationWizardPage