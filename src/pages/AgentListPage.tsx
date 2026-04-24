import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { Layout } from '../components/layout/Layout'
import { Button } from '../components/ui/Button'
import { AssetCard } from '../components/ui/AssetCard'

// Mock data for agents
const agentsData = [
  {
    id: '1',
    name: '客服机器人',
    description: '智能客服助手，自动处理用户咨询，支持多轮对话和意图识别。',
    iconBg: 'bg-brand-50',
    iconText: '客',
    status: 'published' as const,
    toolCount: 5,
    collabCount: 2,
    updatedAt: '2天前',
  },
  {
    id: '2',
    name: '数据分析助手',
    description: '自动分析业务数据，生成可视化报告，支持多种数据源接入。',
    iconBg: 'bg-warning-50',
    iconText: '数',
    status: 'debugging' as const,
    toolCount: 8,
    collabCount: 3,
    updatedAt: '1天前',
  },
  {
    id: '3',
    name: '代码审查专家',
    description: '自动审查代码质量，识别潜在问题并提供优化建议。',
    iconBg: 'bg-success-50',
    iconText: '代',
    status: 'published' as const,
    toolCount: 4,
    collabCount: 1,
    updatedAt: '3天前',
  },
  {
    id: '4',
    name: '文档生成器',
    description: '自动生成项目文档和API文档，支持多种格式导出。',
    iconBg: 'bg-error-50',
    iconText: '文',
    status: 'draft' as const,
    toolCount: 2,
    collabCount: 0,
    updatedAt: '5天前',
  },
  {
    id: '5',
    name: '测试助手',
    description: '自动编写测试用例，生成测试代码，覆盖率分析。',
    iconBg: 'bg-gray-100',
    iconText: '测',
    status: 'published' as const,
    toolCount: 6,
    collabCount: 2,
    updatedAt: '1周前',
  },
  {
    id: '6',
    name: '项目管理助手',
    description: '协助项目管理，任务分配，进度跟踪和风险评估。',
    iconBg: 'bg-brand-50',
    iconText: '项',
    status: 'info' as const,
    toolCount: 3,
    collabCount: 4,
    updatedAt: '4天前',
  },
]

// Tab types
type TabType = 'all' | 'created' | 'collab' | 'published'

// AgentListPage Component
const AgentListPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<TabType>('all')
  const [currentPage, setCurrentPage] = useState(1)

  // Filter agents based on tab
  const filteredAgents = agentsData.filter((agent) => {
    switch (activeTab) {
      case 'created':
        return agent.id === '1' || agent.id === '2' || agent.id === '4'
      case 'collab':
        return agent.collabCount > 0
      case 'published':
        return agent.status === 'published'
      default:
        return true
    }
  })

  // Pagination logic
  const itemsPerPage = 6
  const totalPages = Math.ceil(filteredAgents.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const visibleAgents = filteredAgents.slice(startIndex, startIndex + itemsPerPage)

  const handleUseAgent = (id: string) => {
    navigate(`/agents/${id}`)
  }

  const handleEditAgent = (id: string) => {
    navigate(`/agents/edit/${id}`)
  }

  const handleCreateAgent = () => {
    navigate('/agents/create')
  }

  return (
    <Layout breadcrumb={[{ label: '我的资产' }, { label: '智能体' }]}>
      <div className="p-8 flex flex-col gap-5">
        {/* Title Row */}
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold text-text-primary">我的智能体</h1>
          <Button
            variant="primary"
            icon={<Plus className="w-4 h-4" />}
            onClick={handleCreateAgent}
          >
            创建智能体
          </Button>
        </div>

        {/* Tabs */}
        <div className="flex items-center gap-4">
          {[
            { key: 'all', label: '全部' },
            { key: 'created', label: '我创建的' },
            { key: 'collab', label: '协作的' },
            { key: 'published', label: '已发布' },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as TabType)}
              className={`px-3 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === tab.key
                  ? 'text-brand-500 border-brand-500'
                  : 'text-text-secondary border-transparent hover:text-text-primary'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Grid - 3 columns */}
        <div className="grid grid-cols-3 gap-4">
          {visibleAgents.map((agent) => (
            <AssetCard
              key={agent.id}
              id={agent.id}
              name={agent.name}
              description={agent.description}
              iconBg={agent.iconBg}
              iconText={agent.iconText}
              status={agent.status}
              toolCount={agent.toolCount}
              collabCount={agent.collabCount}
              updatedAt={agent.updatedAt}
              onUse={() => handleUseAgent(agent.id)}
              onEdit={() => handleEditAgent(agent.id)}
            />
          ))}
        </div>

        {/* Empty state */}
        {visibleAgents.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16">
            <p className="text-text-secondary mb-4">暂无智能体</p>
            <Button
              variant="primary"
              icon={<Plus className="w-4 h-4" />}
              onClick={handleCreateAgent}
            >
              创建第一个智能体
            </Button>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-1.5 pt-4">
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`w-8 h-8 rounded-md flex items-center justify-center text-sm font-medium transition-colors ${
                  currentPage === page
                    ? 'bg-brand-500 text-white'
                    : 'bg-white border border-border-subtle text-text-secondary hover:bg-gray-50'
                }`}
              >
                {page}
              </button>
            ))}
          </div>
        )}
      </div>
    </Layout>
  )
}

export default AgentListPage